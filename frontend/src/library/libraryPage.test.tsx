import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { fakeBackend, json, renderApp } from '../test/testUtils'

const ALICE = { id: 1, email: 'alice@example.com', displayName: 'Alice', role: 'USER' }

const base = { notes: null, completedOn: null, addedAt: '2026-10-01T00:00:00Z' }
const WATCHING = {
  ...base, mediaType: 'TV', tmdbId: 1, status: 'WATCHING', rating: null, updatedAt: '2026-10-03T00:00:00Z',
  title: { title: 'Example Show', posterPath: null, releaseDate: '2022-02-17', anime: false },
}
const COMPLETED = {
  ...base, mediaType: 'MOVIE', tmdbId: 2, status: 'COMPLETED', rating: 9, updatedAt: '2026-10-02T00:00:00Z',
  title: { title: 'Example Movie', posterPath: null, releaseDate: '2023-05-01', anime: false },
}
const UNAVAILABLE = {
  ...base, mediaType: 'MOVIE', tmdbId: 3, status: 'ON_HOLD', rating: 6, updatedAt: '2026-10-01T00:00:00Z', title: null,
}

function backend(library: object[]) {
  fakeBackend({
    'GET /api/users/me': () => json(200, ALICE),
    'GET /api/library': () => json(200, library),
  })
}

describe('library page', () => {
  it('lists entries with counts per status, rating and an honest placeholder', async () => {
    backend([WATCHING, COMPLETED, UNAVAILABLE])
    renderApp('/library')

    expect(await screen.findByText('Example Show')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'All 3' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: 'Completed 1' })).toBeInTheDocument()
    expect(screen.getByText('9/10')).toBeInTheDocument()
    expect(screen.getByText('Details unavailable right now')).toBeInTheDocument()
  })

  it('filters by status and type', async () => {
    backend([WATCHING, COMPLETED, UNAVAILABLE])
    renderApp('/library')

    await userEvent.click(await screen.findByRole('button', { name: 'Completed 1' }))
    expect(screen.getByText('Example Movie')).toBeInTheDocument()
    expect(screen.queryByText('Example Show')).not.toBeInTheDocument()

    await userEvent.click(within(screen.getByRole('group', { name: 'Filter by type' })).getByRole('button', { name: 'TV' }))
    expect(screen.getByText('Nothing here with these filters.')).toBeInTheDocument()
  })

  it('reads filters from the address bar', async () => {
    backend([WATCHING, COMPLETED, UNAVAILABLE])
    renderApp('/library?status=WATCHING')

    expect(await screen.findByText('Example Show')).toBeInTheDocument()
    expect(screen.queryByText('Example Movie')).not.toBeInTheDocument()
  })

  it('explains an empty library', async () => {
    backend([])
    renderApp('/library')

    expect(await screen.findByText('Your library is empty.')).toBeInTheDocument()
  })

  it('is linked from the header', async () => {
    backend([])
    renderApp('/')

    expect(await screen.findByRole('link', { name: 'Library' })).toHaveAttribute('href', '/library')
  })
})
