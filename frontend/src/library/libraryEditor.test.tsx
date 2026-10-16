import { screen, waitForElementToBeRemoved, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { fakeBackend, giveCsrfCookie, json, noContent, renderApp } from '../test/testUtils'

const ALICE = { id: 1, email: 'alice@example.com', displayName: 'Alice', role: 'USER' }

const ENTRY = {
  mediaType: 'MOVIE', tmdbId: 1001, status: 'COMPLETED', rating: 7, notes: 'Good.', completedOn: '2026-10-12',
  addedAt: '2026-10-01T00:00:00Z', updatedAt: '2026-10-12T00:00:00Z',
  title: { title: 'Example Movie', posterPath: null, releaseDate: '2023-05-01', anime: false },
}

function backend(routes: Record<string, () => Response> = {}) {
  giveCsrfCookie()
  return fakeBackend({
    'GET /api/users/me': () => json(200, ALICE),
    'GET /api/library': () => json(200, [ENTRY]),
    ...routes,
  })
}

describe('library edit panel', () => {
  it('opens from a card with the current values selected', async () => {
    backend()
    renderApp('/library')

    await userEvent.click(await screen.findByRole('link', { name: 'Edit Example Movie in your library' }))

    const dialog = await screen.findByRole('dialog', { name: 'Example Movie' })
    expect(within(dialog).getByRole('button', { name: 'Completed' })).toHaveAttribute('aria-pressed', 'true')
    expect(within(dialog).getByRole('button', { name: '7 out of 10' })).toHaveAttribute('aria-pressed', 'true')
    expect(within(dialog).getByLabelText('Private notes')).toHaveValue('Good.')
    expect(within(dialog).getByText('Completed on 12 October 2026')).toBeInTheDocument()
  })

  it('saves all changes at once and closes', async () => {
    const requests = backend({ 'PUT /api/library/movie/1001': () => json(200, { ...ENTRY, status: 'WATCHING', rating: 9 }) })
    renderApp('/library?edit=movie-1001')
    const dialog = await screen.findByRole('dialog')

    await userEvent.click(within(dialog).getByRole('button', { name: 'Watching' }))
    await userEvent.click(within(dialog).getByRole('button', { name: '9 out of 10' }))
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }))

    await waitForElementToBeRemoved(() => screen.queryByRole('dialog')) // closes once saved
    expect(requests.find((r) => r.method === 'PUT')?.body).toEqual({ status: 'WATCHING', rating: 9, notes: 'Good.' })
  })

  it('cancel discards changes without calling the server', async () => {
    const requests = backend()
    renderApp('/library?edit=movie-1001')
    const dialog = await screen.findByRole('dialog')

    await userEvent.click(within(dialog).getByRole('button', { name: 'Dropped' }))
    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(requests.some((r) => r.method === 'PUT')).toBe(false)
  })

  it('removes the entry', async () => {
    const requests = backend({ 'DELETE /api/library/movie/1001': noContent })
    renderApp('/library?edit=movie-1001')

    await userEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Remove from library' }))

    await waitForElementToBeRemoved(() => screen.queryByRole('dialog'))
    expect(requests.some((r) => r.method === 'DELETE')).toBe(true)
  })

  it('closes with the Escape key', async () => {
    backend()
    renderApp('/library?edit=movie-1001')
    await screen.findByRole('dialog')

    await userEvent.keyboard('{Escape}')

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })
})
