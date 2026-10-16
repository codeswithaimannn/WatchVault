import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { fakeBackend, giveCsrfCookie, json, renderApp } from '../test/testUtils'

const ALICE = { id: 1, email: 'alice@example.com', displayName: 'Alice', role: 'USER' }

const MOVIE = {
  mediaType: 'MOVIE', tmdbId: 1001, title: 'Example Movie', originalTitle: 'Example Movie', originalLanguage: 'en',
  overview: '', tagline: null, posterPath: null, backdropPath: null, releaseDate: '2023-05-01', ukReleaseDate: null,
  runtimeMinutes: 118, status: 'Released', genres: [], imdbId: null, tvdbId: null, anime: false, seasons: [],
  fetchedAt: new Date().toISOString(),
}

const ENTRY_URL = '/api/library/movie/1001'

function entry(overrides: object = {}) {
  return {
    mediaType: 'MOVIE', tmdbId: 1001, status: 'PLAN_TO_WATCH', rating: null, notes: null, completedOn: null,
    addedAt: '2026-10-16T10:00:00Z', updatedAt: '2026-10-16T10:00:00Z',
    title: { title: 'Example Movie', posterPath: null, releaseDate: '2023-05-01', anime: false },
    ...overrides,
  }
}

function backend(routes: Record<string, () => Response>) {
  giveCsrfCookie()
  return fakeBackend({
    'GET /api/users/me': () => json(200, ALICE),
    'GET /api/titles/movie/1001': () => json(200, MOVIE),
    'GET /api/titles/movie/1001/providers': () =>
      json(200, { region: 'GB', link: null, stream: [], rent: [], buy: [], attribution: 'Streaming data from JustWatch' }),
    'GET /api/library': () => json(200, []),
    ...routes,
  })
}

describe('library panel on the title page', () => {
  it('offers a single Add button that adds the title as Plan to watch', async () => {
    const requests = backend({
      [`GET ${ENTRY_URL}`]: () => json(404, { detail: "This title isn't in your library." }),
      [`PUT ${ENTRY_URL}`]: () => json(200, entry()),
    })
    renderApp('/title/movie/1001')

    await userEvent.click(await screen.findByRole('button', { name: '+ Add to library' }))

    expect(await screen.findByText('In your library')).toBeInTheDocument()
    expect(requests.find((r) => r.method === 'PUT')?.body).toEqual({ status: 'PLAN_TO_WATCH', rating: null, notes: null })
  })

  it('shows status and rating with a link to manage it in the library', async () => {
    backend({ [`GET ${ENTRY_URL}`]: () => json(200, entry({ status: 'COMPLETED', rating: 8 })) })
    renderApp('/title/movie/1001')

    expect(await screen.findByText('Completed · 8/10')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Manage in library →' })).toHaveAttribute('href', '/library?edit=movie-1001')
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument() // no editing controls here
  })

  it('shows why adding failed', async () => {
    backend({
      [`GET ${ENTRY_URL}`]: () => json(404, { detail: "This title isn't in your library." }),
      [`PUT ${ENTRY_URL}`]: () => json(503, { detail: 'TMDB is not responding right now. Please try again shortly.' }),
    })
    renderApp('/title/movie/1001')

    await userEvent.click(await screen.findByRole('button', { name: '+ Add to library' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('TMDB is not responding')
  })
})
