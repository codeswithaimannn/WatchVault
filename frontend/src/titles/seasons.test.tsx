import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { fakeBackend, json, renderApp } from '../test/testUtils'

const ALICE = { id: 1, email: 'alice@example.com', displayName: 'Alice', role: 'USER' }

const SHOW = {
  mediaType: 'TV', tmdbId: 2001, title: 'Example Show', originalTitle: 'Example Show', originalLanguage: 'en',
  overview: '', tagline: null, posterPath: null, backdropPath: null, releaseDate: '2022-02-17',
  ukReleaseDate: null, runtimeMinutes: null, status: 'Returning Series', genres: [], imdbId: null, tvdbId: null,
  anime: false, fetchedAt: new Date().toISOString(),
  seasons: [
    { seasonNumber: 0, name: 'Specials', episodeCount: 1, airDate: null },
    { seasonNumber: 2, name: 'Season 2', episodeCount: 1, airDate: '2025-01-17' },
    { seasonNumber: 1, name: 'Season 1', episodeCount: 2, airDate: '2022-02-17' },
  ],
}

const SEASON_1 = {
  tvTmdbId: 2001, seasonNumber: 1, name: 'Season 1', airDate: '2022-02-17',
  episodes: [
    { episodeNumber: 1, name: 'Pilot', overview: '', airDate: '2022-02-17', runtimeMinutes: 59, stillPath: null },
    { episodeNumber: 2, name: 'TBA', overview: '', airDate: null, runtimeMinutes: null, stillPath: null },
  ],
}

const NO_PROVIDERS = { region: 'GB', link: null, stream: [], rent: [], buy: [], attribution: 'Streaming data from JustWatch' }

describe('seasons', () => {
  it('lists seasons in order with specials last', async () => {
    fakeBackend({
      'GET /api/users/me': () => json(200, ALICE),
      'GET /api/titles/tv/2001': () => json(200, SHOW),
      'GET /api/titles/tv/2001/providers': () => json(200, NO_PROVIDERS),
    })
    renderApp('/title/tv/2001')

    const seasons = within(await screen.findByRole('region', { name: 'Seasons' })).getAllByRole('button')
    expect(seasons.map((button) => button.textContent)).toEqual([
      expect.stringContaining('Season 1'),
      expect.stringContaining('Season 2'),
      expect.stringContaining('Specials'),
    ])
  })

  it('loads episodes only when a season is opened, and is honest about unknowns', async () => {
    const requests = fakeBackend({
      'GET /api/users/me': () => json(200, ALICE),
      'GET /api/titles/tv/2001': () => json(200, SHOW),
      'GET /api/titles/tv/2001/providers': () => json(200, NO_PROVIDERS),
      'GET /api/titles/tv/2001/seasons/1': () => json(200, SEASON_1),
    })
    renderApp('/title/tv/2001')
    const seasonOne = await screen.findByRole('button', { name: /Season 1/ })
    expect(requests.some((r) => r.url.includes('/seasons/'))).toBe(false)

    await userEvent.click(seasonOne)

    expect(seasonOne).toHaveAttribute('aria-expanded', 'true')
    const pilot = within((await screen.findByText('Pilot')).closest('li')!)
    expect(pilot.getByText('17 February 2022')).toBeInTheDocument()
    expect(pilot.getByText('~59 min')).toHaveAttribute('title', 'Estimated runtime')

    const unannounced = within(screen.getByText('TBA').closest('li')!)
    expect(unannounced.getByText('Date not announced')).toBeInTheDocument()
    expect(unannounced.queryByTitle('Estimated runtime')).not.toBeInTheDocument()
  })
})
