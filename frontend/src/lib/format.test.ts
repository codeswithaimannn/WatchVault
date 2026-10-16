import { describe, expect, it } from 'vitest'

import { formatDate, formatRuntime, languageName, yearOf } from './format'
import { posterUrl } from './tmdbImages'

describe('formatDate', () => {
  it('writes dates the British way', () => {
    expect(formatDate('2024-03-01')).toBe('1 March 2024')
  })

  it('returns null for unknown dates instead of guessing', () => {
    expect(formatDate(null)).toBeNull()
    expect(formatDate('not a date')).toBeNull()
  })
})

describe('yearOf', () => {
  it('takes the year from an ISO date', () => {
    expect(yearOf('2024-02-27')).toBe('2024')
    expect(yearOf(null)).toBeNull()
  })
})

describe('formatRuntime', () => {
  it('formats hours and minutes', () => {
    expect(formatRuntime(167)).toBe('2 h 47 min')
    expect(formatRuntime(45)).toBe('45 min')
    expect(formatRuntime(120)).toBe('2 h')
  })

  it('returns null for unknown or zero runtimes', () => {
    expect(formatRuntime(null)).toBeNull()
    expect(formatRuntime(0)).toBeNull()
  })
})

describe('languageName', () => {
  it('turns a language code into its English name', () => {
    expect(languageName('ja')).toBe('Japanese')
    expect(languageName(null)).toBeNull()
  })
})

describe('posterUrl', () => {
  it("builds a TMDB image CDN URL at the requested size", () => {
    expect(posterUrl('/abc.jpg', 'w185')).toBe('https://image.tmdb.org/t/p/w185/abc.jpg')
  })

  it('returns null when there is no poster', () => {
    expect(posterUrl(null)).toBeNull()
  })
})
