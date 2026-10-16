/**
 * Image URLs on TMDB's image CDN. The browser loads these directly: they are plain image
 * files, not API calls, and need no token (owner-approved exception in CLAUDE.md).
 * Sizes are TMDB's fixed set; asking for a small one in grids keeps pages fast.
 */
const IMAGE_BASE_URL = 'https://image.tmdb.org/t/p'

export type PosterSize = 'w185' | 'w342' | 'w500'
export type BackdropSize = 'w780' | 'w1280'
export type LogoSize = 'w45' | 'w92'
export type StillSize = 'w185' | 'w300'

function imageUrl(path: string | null, size: string): string | null {
  return path ? `${IMAGE_BASE_URL}/${size}${path}` : null
}

export const posterUrl = (path: string | null, size: PosterSize = 'w342') => imageUrl(path, size)
export const backdropUrl = (path: string | null, size: BackdropSize = 'w1280') => imageUrl(path, size)
export const logoUrl = (path: string | null, size: LogoSize = 'w92') => imageUrl(path, size)
export const stillUrl = (path: string | null, size: StillSize = 'w300') => imageUrl(path, size)
