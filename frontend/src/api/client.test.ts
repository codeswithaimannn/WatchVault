import { describe, expect, it } from 'vitest'

import { fakeBackend, giveCsrfCookie, json } from '../test/testUtils'
import { ApiError, apiRequest } from './client'

describe('apiRequest', () => {
  it('sends the CSRF token in a header on state-changing requests', async () => {
    giveCsrfCookie('abc123')
    const requests = fakeBackend({ 'POST /api/things': () => json(200, { ok: true }) })

    await apiRequest('/api/things', { method: 'POST', body: { name: 'x' } })

    expect(requests[0].headers['X-XSRF-TOKEN']).toBe('abc123')
    expect(requests[0].body).toEqual({ name: 'x' })
  })

  it('does not send the CSRF header on GET', async () => {
    const requests = fakeBackend({ 'GET /api/things': () => json(200, []) })

    await apiRequest('/api/things')

    expect(requests[0].headers['X-XSRF-TOKEN']).toBeUndefined()
  })

  it('turns a problem-details body into an ApiError with field errors', async () => {
    giveCsrfCookie()
    fakeBackend({
      'POST /api/things': () =>
        json(400, { detail: 'Some fields are invalid.', errors: { email: 'Enter a valid email address.' } }),
    })

    const error = await apiRequest('/api/things', { method: 'POST', body: {} }).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      status: 400,
      message: 'Some fields are invalid.',
      fieldErrors: { email: 'Enter a valid email address.' },
    })
  })

  it('reports a friendly message when the server cannot be reached', async () => {
    fakeBackend({}) // every request throws, like a network failure

    await expect(apiRequest('/api/things')).rejects.toThrow('Cannot reach the server')
  })
})
