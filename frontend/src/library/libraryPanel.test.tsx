import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { fakeBackend, giveCsrfCookie, json, noContent, renderApp } from '../test/testUtils'

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
    mediaType: 'MOVIE', tmdbId: 1001, status: 'WATCHING', rating: null, notes: null, completedOn: null,
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

describe('library panel', () => {
  it('adds a title by choosing a status', async () => {
    const requests = backend({
      [`GET ${ENTRY_URL}`]: () => json(404, { detail: "This title isn't in your library." }),
      [`PUT ${ENTRY_URL}`]: () => json(200, entry({ status: 'PLAN_TO_WATCH' })),
    })
    renderApp('/title/movie/1001')

    const status = await screen.findByRole('combobox', { name: 'Status' })
    expect(status).toHaveValue('') // "Add to library…"
    await userEvent.selectOptions(status, 'Plan to watch')

    expect(await screen.findByRole('button', { name: 'Remove from library' })).toBeInTheDocument()
    const put = requests.find((r) => r.method === 'PUT')
    expect(put?.body).toEqual({ status: 'PLAN_TO_WATCH', rating: null, notes: null })
  })

  it('shows the completion date and saves a rating without losing other fields', async () => {
    const requests = backend({
      [`GET ${ENTRY_URL}`]: () => json(200, entry({ status: 'COMPLETED', completedOn: '2026-10-12', notes: 'Great.' })),
      [`PUT ${ENTRY_URL}`]: () => json(200, entry({ status: 'COMPLETED', rating: 9, notes: 'Great.' })),
    })
    renderApp('/title/movie/1001')

    expect(await screen.findByText('Completed on 12 October 2026')).toBeInTheDocument()
    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Your rating' }), '9/10')

    const put = requests.find((r) => r.method === 'PUT')
    expect(put?.body).toEqual({ status: 'COMPLETED', rating: 9, notes: 'Great.' })
  })

  it('saves notes', async () => {
    const requests = backend({
      [`GET ${ENTRY_URL}`]: () => json(200, entry()),
      [`PUT ${ENTRY_URL}`]: () => json(200, entry({ notes: 'Rewatch the finale.', updatedAt: '2026-10-16T11:00:00Z' })),
    })
    renderApp('/title/movie/1001')

    await userEvent.type(await screen.findByLabelText('Private notes'), 'Rewatch the finale.')
    await userEvent.click(screen.getByRole('button', { name: 'Save notes' }))

    expect(requests.find((r) => r.method === 'PUT')?.body).toMatchObject({ notes: 'Rewatch the finale.' })
  })

  it('removes a title', async () => {
    const requests = backend({
      [`GET ${ENTRY_URL}`]: () => json(200, entry()),
      [`DELETE ${ENTRY_URL}`]: noContent,
    })
    renderApp('/title/movie/1001')

    await userEvent.click(await screen.findByRole('button', { name: 'Remove from library' }))

    expect(await screen.findByRole('option', { name: 'Add to library…' })).toBeInTheDocument()
    expect(requests.some((r) => r.method === 'DELETE')).toBe(true)
  })

  it('shows why saving failed', async () => {
    backend({
      [`GET ${ENTRY_URL}`]: () => json(404, { detail: "This title isn't in your library." }),
      [`PUT ${ENTRY_URL}`]: () => json(503, { detail: 'TMDB is not responding right now. Please try again shortly.' }),
    })
    renderApp('/title/movie/1001')

    await userEvent.selectOptions(await screen.findByRole('combobox', { name: 'Status' }), 'Watching')

    expect(await screen.findByRole('alert')).toHaveTextContent('TMDB is not responding')
  })
})
