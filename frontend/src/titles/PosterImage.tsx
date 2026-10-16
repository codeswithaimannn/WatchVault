import { posterUrl, type PosterSize } from '../lib/tmdbImages'

interface PosterImageProps {
  path: string | null
  title: string
  size?: PosterSize
  className?: string
}

/** A 2:3 poster, or a neutral placeholder when TMDB has none. Lazy-loaded: off-screen posters wait. */
export function PosterImage({ path, title, size = 'w342', className = '' }: PosterImageProps) {
  const url = posterUrl(path, size)
  const frame = `aspect-2/3 overflow-hidden rounded-lg bg-slate-800/60 ring-1 ring-white/10 ${className}`

  if (!url) {
    return (
      <div className={`${frame} grid place-items-center p-3 text-center`}>
        <span className="text-xs text-slate-500">No poster</span>
      </div>
    )
  }
  return (
    <div className={frame}>
      <img src={url} alt={`Poster for ${title}`} loading="lazy" className="size-full object-cover" />
    </div>
  )
}
