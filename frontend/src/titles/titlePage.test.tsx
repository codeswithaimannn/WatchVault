import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { fakeBackend, json, renderApp } from '../test/testUtils'

const ALICE = { id: 1, email: 'alice@example.com', displayName: 'Alice', role: 'USER' }

const DUNE = {
  mediaType: 'MOVIE', tmdbId: 693134, title: 'Example Epic', originalTitle: 'Example Epic', originalLanguage: 'en',
  overview: 'A long journey.', tagline: 'Long live the example.', posterPath: '/p.jpg', backdropPath: '/b.jpg',
  releaseDate: '2024-02-27', ukReleaseDate: '2024-03-01', runtimeMinutes: 167, status: 'Released',
  genres: [{ id: 878, name: 'Science Fiction' }], imdbId: 'tt1', tvdbId: null, anime: false, seasons: [],
  fetchedAt: new Date().toISOString(),
}

const NO_PROVIDERS = { region: 'GB', link: null, stream: [], rent: [], buy: [], attribution: 'Streaming data from JustWatch' }

function backend(title: object, extra: Record<string, () => Response> = {}) {
  return fakeBackend({
    'GET /api/users/me': () => json(200, ALICE),
    'GET /api/titles/movie/693134': () => json(200, title),
    'GET /api/titles/movie/693134/providers': () => json(200, NO_PROVIDERS),
    ...extra,
  })
}

describe('title page', () => {
  it('shows the main facts with British dates and runtime', async () => {
    backend(DUNE)
    renderApp('/title/movie/693134')

    expect(await screen.findByRole('heading', { name: /Example Epic/ })).toBeInTheDocument()
    expect(screen.getByText('27 February 2024')).toBeInTheDocument() // labelled "Release date", not UK
    expect(screen.getByText('UK cinema release')).toBeInTheDocument()
    expect(screen.getByText('1 March 2024')).toBeInTheDocument()
    expect(screen.getByText('2 h 47 min')).toBeInTheDocument()
    expect(screen.getByText('Science Fiction')).toBeInTheDocument()
  })

  it('leaves out the UK date when TMDB does not know it', async () => {
    backend({ ...DUNE, ukReleaseDate: null })
    renderApp('/title/movie/693134')

    expect(await screen.findByText('27 February 2024')).toBeInTheDocument()
    expect(screen.queryByText('UK cinema release')).not.toBeInTheDocument()
  })

  it('says when saved details are old', async () => {
    backend({ ...DUNE, fetchedAt: '2026-01-05T10:00:00Z' })
    renderApp('/title/movie/693134')

    expect(await screen.findByRole('note')).toHaveTextContent('Details last updated 5 January 2026')
  })

  it('does not show the notice for fresh details', async () => {
    backend(DUNE)
    renderApp('/title/movie/693134')

    await screen.findByRole('heading', { name: /Example Epic/ })
    expect(screen.queryByRole('note')).not.toBeInTheDocument()
  })

  it('explains an unknown title', async () => {
    fakeBackend({
      'GET /api/users/me': () => json(200, ALICE),
      'GET /api/titles/movie/999': () => json(404, { detail: "We couldn't find that title." }),
      'GET /api/titles/movie/999/providers': () => json(404, { detail: "We couldn't find that title." }),
    })
    renderApp('/title/movie/999')

    expect(await screen.findByText("We couldn't find that title.")).toBeInTheDocument()
  })

  it('treats a malformed address as not found without calling the API', async () => {
    const requests = fakeBackend({ 'GET /api/users/me': () => json(200, ALICE) })
    renderApp('/title/anime/abc')

    expect(await screen.findByText('This scene was cut')).toBeInTheDocument()
    expect(requests.some((r) => r.url.startsWith('/api/titles'))).toBe(false)
  })
})
