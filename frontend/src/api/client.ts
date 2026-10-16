/**
 * The one place that calls fetch. Every request goes to our own backend (/api/...);
 * the browser never calls TMDB or TVmaze directly.
 */

const CSRF_COOKIE = 'XSRF-TOKEN'
const CSRF_HEADER = 'X-XSRF-TOKEN'
const CSRF_ENDPOINT = '/api/auth/csrf'
const STATE_CHANGING_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

const GENERIC_ERROR = 'Something went wrong. Please try again.'
const NETWORK_ERROR = 'Cannot reach the server. Check your connection and try again.'

/** A failed API call, with the backend's safe message and any per-field messages. */
export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

/** The JSON error body Spring sends (RFC 9457 "problem details"). */
interface ProblemDetail {
  detail?: unknown
  errors?: unknown
}

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

interface RequestOptions {
  method?: HttpMethod
  body?: unknown
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const method = options.method ?? 'GET'
  const headers: Record<string, string> = {}
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (STATE_CHANGING_METHODS.has(method)) {
    headers[CSRF_HEADER] = await getCsrfToken()
  }

  let response: Response
  try {
    response = await fetch(path, {
      method,
      headers,
      credentials: 'same-origin', // send our cookies; we never call other sites
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    })
  } catch {
    throw new ApiError(0, NETWORK_ERROR)
  }

  if (!response.ok) {
    throw await toApiError(response)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

/**
 * Double-submit CSRF: Spring puts a token in the XSRF-TOKEN cookie and checks that we send
 * the same value back in a header. A malicious site can make the browser send our cookies,
 * but it cannot read them, so it cannot copy the token into the header.
 */
async function getCsrfToken(): Promise<string> {
  let token = readCookie(CSRF_COOKIE)
  if (!token) {
    await fetch(CSRF_ENDPOINT, { credentials: 'same-origin' }).catch(() => undefined)
    token = readCookie(CSRF_COOKIE)
  }
  if (!token) {
    throw new ApiError(0, NETWORK_ERROR)
  }
  return token
}

function readCookie(name: string): string | undefined {
  const prefix = `${name}=`
  const match = document.cookie.split('; ').find((cookie) => cookie.startsWith(prefix))
  return match ? decodeURIComponent(match.slice(prefix.length)) : undefined
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const problem = (await response.json()) as ProblemDetail
    const message = typeof problem.detail === 'string' ? problem.detail : GENERIC_ERROR
    return new ApiError(response.status, message, toFieldErrors(problem.errors))
  } catch {
    // Not JSON (e.g. an empty 401 from Spring Security).
    return new ApiError(response.status, GENERIC_ERROR)
  }
}

function toFieldErrors(errors: unknown): Record<string, string> {
  if (typeof errors !== 'object' || errors === null) {
    return {}
  }
  return Object.fromEntries(
    Object.entries(errors).filter((entry): entry is [string, string] => typeof entry[1] === 'string'),
  )
}

/** A message that is always safe to show the user. */
export function errorMessage(error: unknown): string {
  return error instanceof ApiError ? error.message : GENERIC_ERROR
}
