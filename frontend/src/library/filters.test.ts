import { describe, expect, it } from 'vitest'

import type { LibraryEntry } from '../api/library'
import { countByStatus, filterAndSort } from './filters'

function entry(id: number, overrides: Partial<LibraryEntry>): LibraryEntry {
  return {
    mediaType: 'MOVIE', tmdbId: id, status: 'WATCHING', rating: null, notes: null, completedOn: null,
    addedAt: '2026-10-01T00:00:00Z', updatedAt: '2026-10-01T00:00:00Z',
    title: { title: `Title ${id}`, posterPath: null, releaseDate: null, anime: false },
    ...overrides,
  }
}

const zulu = entry(1, { title: { title: 'Zulu', posterPath: null, releaseDate: null, anime: false }, rating: 6, updatedAt: '2026-10-03T00:00:00Z' })
const alpha = entry(2, { title: { title: 'alpha', posterPath: null, releaseDate: null, anime: false }, rating: 9, status: 'COMPLETED', updatedAt: '2026-10-01T00:00:00Z' })
const anime = entry(3, { mediaType: 'TV', title: { title: 'Mecha', posterPath: null, releaseDate: null, anime: true }, updatedAt: '2026-10-02T00:00:00Z' })
const unknown = entry(4, { title: null, rating: 6, updatedAt: '2026-10-04T00:00:00Z' })
const all = [zulu, alpha, anime, unknown]

const titles = (entries: LibraryEntry[]) => entries.map((e) => e.title?.title ?? '(unavailable)')

describe('filterAndSort', () => {
  it('sorts by most recently updated by default', () => {
    expect(titles(filterAndSort(all, { status: 'ALL', type: 'ALL', sort: 'UPDATED' }))).toEqual([
      '(unavailable)', 'Zulu', 'Mecha', 'alpha',
    ])
  })

  it('sorts titles A-Z ignoring case, with unavailable titles last', () => {
    expect(titles(filterAndSort(all, { status: 'ALL', type: 'ALL', sort: 'TITLE' }))).toEqual([
      'alpha', 'Mecha', 'Zulu', '(unavailable)',
    ])
  })

  it('sorts by rating, unrated last, ties by most recent', () => {
    expect(titles(filterAndSort(all, { status: 'ALL', type: 'ALL', sort: 'RATING' }))).toEqual([
      'alpha', '(unavailable)', 'Zulu', 'Mecha',
    ])
  })

  it('filters by status and by type', () => {
    expect(titles(filterAndSort(all, { status: 'COMPLETED', type: 'ALL', sort: 'UPDATED' }))).toEqual(['alpha'])
    expect(titles(filterAndSort(all, { status: 'ALL', type: 'ANIME', sort: 'UPDATED' }))).toEqual(['Mecha'])
    expect(titles(filterAndSort(all, { status: 'ALL', type: 'TV', sort: 'UPDATED' }))).toEqual(['Mecha'])
  })

  it('does not change the original list', () => {
    const before = [...all]
    filterAndSort(all, { status: 'ALL', type: 'ALL', sort: 'TITLE' })
    expect(all).toEqual(before)
  })
})

describe('countByStatus', () => {
  it('counts each status within the chosen type', () => {
    const counts = countByStatus(all, 'MOVIE')
    expect(counts.ALL).toBe(3)
    expect(counts.WATCHING).toBe(2)
    expect(counts.COMPLETED).toBe(1)
  })
})
