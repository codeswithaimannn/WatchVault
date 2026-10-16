import type { MediaType } from '../api/titles'

/** "Anime" wins over "TV"/"Movie": the anime rule (Animation + Japanese) is decided by the backend. */
export function MediaBadge({ mediaType, anime }: { mediaType: MediaType; anime: boolean }) {
  const label = anime ? 'Anime' : mediaType === 'MOVIE' ? 'Movie' : 'TV'
  const colour = anime
    ? 'border-teal-400/30 text-teal-300'
    : mediaType === 'MOVIE'
      ? 'border-sky-400/30 text-sky-300'
      : 'border-slate-400/30 text-slate-300'

  return (
    <span className={`rounded border bg-ink/70 px-1.5 py-0.5 text-[11px] font-medium tracking-wide ${colour}`}>
      {label}
    </span>
  )
}
