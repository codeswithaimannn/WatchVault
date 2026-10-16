import { apiRequest } from './client'

/** Mirrors the backend DTOs in com.watchvault.title. Dates are "yyyy-MM-dd" strings or null. */
export type MediaType = 'MOVIE' | 'TV'

/** How a media type appears in our URLs and API paths. */
export type MediaPath = 'movie' | 'tv'

export interface Genre {
  id: number
  name: string
}

export interface TitleSummary {
  mediaType: MediaType
  tmdbId: number
  title: string
  originalLanguage: string | null
  overview: string | null
  posterPath: string | null
  releaseDate: string | null
  genreIds: number[]
  anime: boolean
}

export interface SearchResults {
  page: number
  totalPages: number
  results: TitleSummary[]
}

export interface SeasonSummary {
  seasonNumber: number
  name: string
  episodeCount: number
  airDate: string | null
}

export interface TitleDetails {
  mediaType: MediaType
  tmdbId: number
  title: string
  originalTitle: string | null
  originalLanguage: string | null
  overview: string | null
  tagline: string | null
  posterPath: string | null
  backdropPath: string | null
  /** TMDB's main date (premiere / first air date), not necessarily the UK one. */
  releaseDate: string | null
  /** Movies only: first UK cinema release, or null if unknown. */
  ukReleaseDate: string | null
  runtimeMinutes: number | null
  status: string | null
  genres: Genre[]
  imdbId: string | null
  tvdbId: number | null
  anime: boolean
  seasons: SeasonSummary[]
  /** When this copy came from TMDB (ISO timestamp). */
  fetchedAt: string
}

export interface Episode {
  episodeNumber: number
  name: string | null
  overview: string | null
  airDate: string | null
  /** TMDB's estimate, not recorded viewing time. */
  runtimeMinutes: number | null
  stillPath: string | null
}

export interface SeasonDetails {
  tvTmdbId: number
  seasonNumber: number
  name: string
  airDate: string | null
  episodes: Episode[]
}

export interface Provider {
  id: number
  name: string
  logoPath: string | null
}

export interface WatchProviders {
  region: string
  link: string | null
  stream: Provider[]
  rent: Provider[]
  buy: Provider[]
  attribution: string
}

export function toPath(mediaType: MediaType): MediaPath {
  return mediaType === 'MOVIE' ? 'movie' : 'tv'
}

export const titlesApi = {
  search: (query: string, page: number) =>
    apiRequest<SearchResults>(`/api/search?q=${encodeURIComponent(query)}&page=${page}`),

  details: (media: MediaPath, tmdbId: number) => apiRequest<TitleDetails>(`/api/titles/${media}/${tmdbId}`),

  providers: (media: MediaPath, tmdbId: number) =>
    apiRequest<WatchProviders>(`/api/titles/${media}/${tmdbId}/providers`),

  season: (tvTmdbId: number, seasonNumber: number) =>
    apiRequest<SeasonDetails>(`/api/titles/tv/${tvTmdbId}/seasons/${seasonNumber}`),
}
