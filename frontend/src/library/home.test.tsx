import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { fakeBackend, json, renderApp } from '../test/testUtils'

const ALICE = { id: 1, email: 'alice@example.com', displayName: 'Alice', role: 'USER' }
const base = { rating: null, notes: null, completedOn: null, addedAt: '2026-10-01T00:00:00Z' }

function entry(tmdbId: number, status: string, title: string, updatedAt: string) {
  return {
    ...base, mediaType: 'TV', tmdbId, status, updatedAt,
    title: { title, posterPath: null, releaseDate: null, anime: false },
  }
}

describe('home', () => {
  it('shows status counts and continue watching from the library', async () => {
    fakeBackend({
      'GET /api/users/me': () => json(200, ALICE),
      'GET /api/library': () =>
        json(200, [
          entry(1, 'WATCHING', 'Newest Show', '2026-10-03T00:00:00Z'),
          entry(2, 'COMPLETED', 'Finished Show', '2026-10-02T00:00:00Z'),
          entry(3, 'WATCHING', 'Older Show', '2026-10-01T00:00:00Z'),
        ]),
    })
    renderApp('/')

    expect(await screen.findByRole('heading', { name: 'Continue watching' })).toBeInTheDocument()
    const cards = screen.getAllByRole('link', { name: /Show/ })
    expect(cards.map((card) => card.textContent)).toEqual([
      expect.stringContaining('Newest Show'),
      expect.stringContaining('Older Show'),
    ]) // only Watching, newest first
    expect(screen.getByRole('link', { name: /2\s*Watching/ })).toHaveAttribute('href', '/library?status=WATCHING')
  })

  it('welcomes a new user with suggestions when the library is empty', async () => {
    fakeBackend({
      'GET /api/users/me': () => json(200, ALICE),
      'GET /api/library': () => json(200, []),
    })
    renderApp('/')

    expect(await screen.findByText('Your library is empty for now.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Severance' })).toHaveAttribute('href', '/search?q=Severance')
  })
})
