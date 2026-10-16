import type { MediaPath, MediaType } from '../api/titles'
import { toPath } from '../api/titles'

/**
 * Which entry the library's edit panel shows, kept in the URL as ?edit=movie-1001,
 * so the title page can link straight to it and Back closes it.
 */
export function editKey(mediaType: MediaType, tmdbId: number): string {
  return `${toPath(mediaType)}-${tmdbId}`
}

export function parseEditKey(value: string | null): { media: MediaPath; tmdbId: number } | null {
  const match = value?.match(/^(movie|tv)-(\d+)$/)
  return match ? { media: match[1] as MediaPath, tmdbId: Number(match[2]) } : null
}

export function editHref(mediaType: MediaType, tmdbId: number): string {
  return `/library?edit=${editKey(mediaType, tmdbId)}`
}
