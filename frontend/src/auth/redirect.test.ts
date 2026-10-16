import { describe, expect, it } from 'vitest'

import { redirectTargetAfterLogin } from './redirect'

describe('redirectTargetAfterLogin', () => {
  it('goes back to the page that required login', () => {
    expect(redirectTargetAfterLogin({ from: '/library' })).toBe('/library')
  })

  it('goes home when there is no previous page', () => {
    expect(redirectTargetAfterLogin(null)).toBe('/')
    expect(redirectTargetAfterLogin({})).toBe('/')
  })

  it('never redirects to another website', () => {
    expect(redirectTargetAfterLogin({ from: '//evil.example' })).toBe('/')
    expect(redirectTargetAfterLogin({ from: 'https://evil.example' })).toBe('/')
  })
})
