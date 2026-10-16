import { Link } from 'react-router'

import { toPath, type TitleSummary } from '../api/titles'
import { yearOf } from '../lib/format'
import { MediaBadge } from './MediaBadge'
import { PosterImage } from './PosterImage'

/** One search result: poster, title, year and type. Links to the detail page. */
export function TitleCard({ title }: { title: TitleSummary }) {
  const year = yearOf(title.releaseDate)

  return (
    <Link
      to={`/title/${toPath(title.mediaType)}/${title.tmdbId}`}
      className="group flex flex-col gap-2.5 rounded-xl p-1.5 transition hover:bg-white/5 focus-visible:bg-white/5 focus-visible:outline-none"
    >
      <div className="relative">
        <PosterImage
          path={title.posterPath}
          title={title.title}
          size="w342"
          className="transition group-hover:ring-sky-400/40"
        />
        <div className="absolute top-2 left-2">
          <MediaBadge mediaType={title.mediaType} anime={title.anime} />
        </div>
      </div>
      <div>
        <p className="line-clamp-2 text-sm font-medium text-slate-100">{title.title}</p>
        <p className="text-xs text-slate-500">{year ?? 'Date unknown'}</p>
      </div>
    </Link>
  )
}
