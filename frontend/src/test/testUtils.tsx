import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { vi } from 'vitest'

import { App } from '../App'

/** Renders the whole app at a given URL, with a fresh query cache and an in-memory router. */
export function renderApp(url: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[url]}>
        <App />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

/** Pretends the backend already gave us a CSRF cookie, as GET /api/auth/csrf would. */
export function giveCsrfCookie(token = 'test-csrf-token') {
  document.cookie = `XSRF-TOKEN=${token}; path=/`
}

export interface RecordedRequest {
  method: string
  url: string
  headers: Record<string, string>
  body: unknown
}

type FakeRoutes = Record<string, () => Response>

/**
 * Replaces fetch with a fake backend. Keys are "METHOD /path"; each value builds a fresh
 * Response (a Response body can only be read once). Unknown routes fail loudly.
 * Returns the list of requests made, so tests can check what was sent.
 */
export function fakeBackend(routes: FakeRoutes): RecordedRequest[] {
  const requests: RecordedRequest[] = []
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: RequestInfo | URL, init: RequestInit = {}) => {
      const method = init.method ?? 'GET'
      const url = String(input)
      requests.push({
        method,
        url,
        headers: (init.headers ?? {}) as Record<string, string>,
        body: typeof init.body === 'string' ? JSON.parse(init.body) : undefined,
      })
      const route = routes[`${method} ${url}`]
      if (!route) {
        throw new Error(`No fake response for ${method} ${url}`)
      }
      return route()
    }),
  )
  return requests
}

export function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

export function noContent(): Response {
  return new Response(null, { status: 204 })
}

/** Spring Security's 401 has no body. */
export function unauthorized(): Response {
  return new Response(null, { status: 401 })
}
