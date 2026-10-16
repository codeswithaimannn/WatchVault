import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { fakeBackend, json, renderApp, unauthorized } from '../test/testUtils'

const ALICE = { id: 1, email: 'alice@example.com', displayName: 'Alice', role: 'USER' }

const MOVIE = {
  mediaType: 'MOVIE', tmdbId: 1001, title: 'Example Movie', originalTitle: 'Example Movie', originalLanguage: 'en',
  overview: '', tagline: null, posterPath: null, backdropPath: null, releaseDate: '2023-05-01', ukReleaseDate: null,
  runtimeMinutes: 118, status: 'Released', genres: [], imdbId: null, tvdbId: null, anime: false, seasons: [],
  fetchedAt: new Date().toISOString(),
}

const ATTRIBUTION = 'Streaming data from JustWatch'

function backend(providers: object) {
  fakeBackend({
    'GET /api/users/me': () => json(200, ALICE),
    'GET /api/titles/movie/1001': () => json(200, MOVIE),
    'GET /api/titles/movie/1001/providers': () => json(200, providers),
  })
}

describe('where to watch', () => {
  it('groups UK providers and always credits JustWatch', async () => {
    backend({
      region: 'GB', link: 'https://www.themoviedb.org/movie/1001/watch?locale=GB',
      stream: [{ id: 8, name: 'Example Stream', logoPath: '/s.png' }],
      rent: [],
      buy: [{ id: 2, name: 'Example Store', logoPath: null }],
      attribution: ATTRIBUTION,
    })
    renderApp('/title/movie/1001')

    const stream = await screen.findByRole('list', { name: 'Stream' })
    expect(within(stream).getByText('Example Stream')).toBeInTheDocument()
    expect(screen.getByRole('list', { name: 'Buy' })).toBeInTheDocument()
    expect(screen.queryByRole('list', { name: 'Rent' })).not.toBeInTheDocument() // empty groups hidden
    expect(screen.getByRole('link', { name: 'JustWatch' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /All options on TMDB/ })).toHaveAttribute('rel', 'noopener noreferrer')
  })

  it('says plainly when a title is not available in the UK, still crediting JustWatch', async () => {
    backend({ region: 'GB', link: null, stream: [], rent: [], buy: [], attribution: ATTRIBUTION })
    renderApp('/title/movie/1001')

    expect(await screen.findByText('Not currently available to stream, rent or buy in the UK.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'JustWatch' })).toBeInTheDocument()
  })
})

describe('about page', () => {
  it('shows the exact TMDB notice and the other credits, even when logged out', async () => {
    fakeBackend({ 'GET /api/users/me': unauthorized })
    renderApp('/about')

    expect(
      await screen.findByText('This product uses the TMDB API but is not endorsed or certified by TMDB.'),
    ).toBeInTheDocument()
    expect(screen.getByAltText('The Movie Database (TMDB) logo')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'CC BY-SA 4.0' })).toBeInTheDocument()
  })
})
