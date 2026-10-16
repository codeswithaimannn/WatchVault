import { ApiError, apiRequest } from './client'
import type { MediaPath, MediaType } from './titles'

/** Mirrors the backend's LibraryStatus enum. */
export type LibraryStatus = 'PLAN_TO_WATCH' | 'WATCHING' | 'COMPLETED' | 'ON_HOLD' | 'DROPPED'

export interface LibraryTitle {
  title: string
  posterPath: string | null
  releaseDate: string | null
  anime: boolean
}

export interface LibraryEntry {
  mediaType: MediaType
  tmdbId: number
  status: LibraryStatus
  rating: number | null
  notes: string | null
  completedOn: string | null
  addedAt: string
  updatedAt: string
  /** null when the title's details can't be loaded right now (TMDB down and nothing saved). */
  title: LibraryTitle | null
}

export interface SaveLibraryEntry {
  status: LibraryStatus
  rating: number | null
  notes: string | null
}

export const libraryApi = {
  list: () => apiRequest<LibraryEntry[]>('/api/library'),

  /** The entry for one title, or null if it isn't in the library (the API answers 404). */
  get: async (media: MediaPath, tmdbId: number): Promise<LibraryEntry | null> => {
    try {
      return await apiRequest<LibraryEntry>(`/api/library/${media}/${tmdbId}`)
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) return null
      throw error
    }
  },

  save: (media: MediaPath, tmdbId: number, entry: SaveLibraryEntry) =>
    apiRequest<LibraryEntry>(`/api/library/${media}/${tmdbId}`, { method: 'PUT', body: entry }),

  remove: (media: MediaPath, tmdbId: number) =>
    apiRequest<void>(`/api/library/${media}/${tmdbId}`, { method: 'DELETE' }),
}
