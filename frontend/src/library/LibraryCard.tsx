import { Link } from 'react-router'

import type { LibraryEntry } from '../api/library'
import { toPath } from '../api/titles'
import { yearOf } from '../lib/format'
import { MediaBadge } from '../titles/MediaBadge'
import { PosterImage } from '../titles/PosterImage'
import { statusLabel } from './statuses'

/** One library entry. Still shows the user's own data when the title details are unavailable. */
export function LibraryCard({ entry }: { entry: LibraryEntry }) {
  const name = entry.title?.title ?? 'Details unavailable right now'

  return (
    <Link
      to={`/title/${toPath(entry.mediaType)}/${entry.tmdbId}`}
      className="group flex flex-col gap-2.5 rounded-xl p-1.5 transition hover:bg-white/5 focus-visible:bg-white/5 focus-visible:outline-none"
    >
      <div className="relative">
        <PosterImage path={entry.title?.posterPath ?? null} title={name} size="w342" className="transition group-hover:ring-sky-400/40" />
        <div className="absolute top-2 left-2">
          <MediaBadge mediaType={entry.mediaType} anime={entry.title?.anime ?? false} />
        </div>
        {entry.rating !== null && (
          <span className="absolute right-2 bottom-2 rounded bg-ink/80 px-1.5 py-0.5 text-xs font-semibold text-sky-300 tabular-nums">
            {entry.rating}/10
          </span>
        )}
      </div>
      <div>
        <p className={`line-clamp-2 text-sm font-medium ${entry.title ? 'text-slate-100' : 'text-slate-500 italic'}`}>{name}</p>
        <p className="text-xs text-slate-500">
          {statusLabel(entry.status)}
          {entry.title && yearOf(entry.title.releaseDate) && ` · ${yearOf(entry.title.releaseDate)}`}
        </p>
      </div>
    </Link>
  )
}
