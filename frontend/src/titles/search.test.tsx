import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { fakeBackend, json, renderApp } from '../test/testUtils'

const ALICE = { id: 1, email: 'alice@example.com', displayName: 'Alice', role: 'USER' }

const MOVIE = {
  mediaType: 'MOVIE', tmdbId: 1001, title: 'Example Movie', originalLanguage: 'en', overview: '',
  posterPath: '/movie.jpg', releaseDate: '2023-05-01', genreIds: [28], anime: false,
}
const ANIME = {
  mediaType: 'TV', tmdbId: 2001, title: 'Example Anime', originalLanguage: 'ja', overview: '',
  posterPath: null, releaseDate: null, genreIds: [16], anime: true,
}

function page(number: number, totalPages: number, results: unknown[]) {
  return () => json(200, { page: number, totalPages, results })
}

describe('search', () => {
  it('searches from the header and shows results with badges', async () => {
    fakeBackend({
      'GET /api/users/me': () => json(200, ALICE),
      'GET /api/search?q=example&page=1': page(1, 1, [MOVIE, ANIME]),
    })
    renderApp('/')

    await userEvent.type(await screen.findByRole('searchbox'), 'example{Enter}')

    expect(await screen.findByText('Example Movie')).toBeInTheDocument()
    expect(screen.getByText('Example Anime')).toBeInTheDocument()
    expect(screen.getByText('Anime')).toBeInTheDocument()
    expect(screen.getByText('No poster')).toBeInTheDocument() // anime has no poster
    expect(screen.getByText('Date unknown')).toBeInTheDocument() // and no date: we don't guess
    expect(screen.getByRole('link', { name: /Example Movie/ })).toHaveAttribute('href', '/title/movie/1001')
  })

  it('says so when nothing matches', async () => {
    fakeBackend({
      'GET /api/users/me': () => json(200, ALICE),
      'GET /api/search?q=zzzz&page=1': page(1, 0, []),
    })
    renderApp('/search?q=zzzz')

    expect(await screen.findByText(/No films or series found/)).toBeInTheDocument()
  })

  it('shows the server message with a retry when search fails', async () => {
    fakeBackend({
      'GET /api/users/me': () => json(200, ALICE),
      'GET /api/search?q=down&page=1': () =>
        json(503, { detail: 'TMDB is not responding right now. Please try again shortly.' }),
    })
    renderApp('/search?q=down')

    expect(await screen.findByText(/TMDB is not responding/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument()
  })

  it('loads the next page on request', async () => {
    fakeBackend({
      'GET /api/users/me': () => json(200, ALICE),
      'GET /api/search?q=example&page=1': page(1, 2, [MOVIE]),
      'GET /api/search?q=example&page=2': page(2, 2, [ANIME]),
    })
    renderApp('/search?q=example')

    await userEvent.click(await screen.findByRole('button', { name: 'Load more' }))

    expect(await screen.findByText('Example Anime')).toBeInTheDocument()
    expect(screen.getByText('Example Movie')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Load more' })).not.toBeInTheDocument() // last page
  })
})
