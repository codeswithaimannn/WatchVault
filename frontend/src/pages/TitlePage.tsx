import { useState } from 'react'
import { Link, useParams } from 'react-router'

import { ApiError, errorMessage } from '../api/client'
import type { MediaPath, TitleDetails } from '../api/titles'
import { StatusMessage } from '../components/StatusMessage'
import { formatDate, formatRuntime, languageName, yearOf } from '../lib/format'
import { backdropUrl } from '../lib/tmdbImages'
import { MediaBadge } from '../titles/MediaBadge'
import { PosterImage } from '../titles/PosterImage'
import { SeasonList } from '../titles/SeasonList'
import { useTitle } from '../titles/titleHooks'
import { NotFoundPage } from './NotFoundPage'

/** Must match the backend's watchvault.cache.title-refresh-after (7 days). */
const STALE_AFTER_DAYS = 7
const MS_PER_DAY = 24 * 60 * 60 * 1000

/** /title/:mediaType/:tmdbId. Checks the URL before calling the API. */
export function TitlePage() {
  const { mediaType, tmdbId } = useParams()
  const id = Number(tmdbId)
  if ((mediaType !== 'movie' && mediaType !== 'tv') || !Number.isInteger(id) || id <= 0) {
    return <NotFoundPage />
  }
  return <TitleView media={mediaType} tmdbId={id} />
}

function TitleView({ media, tmdbId }: { media: MediaPath; tmdbId: number }) {
  const title = useTitle(media, tmdbId)

  if (title.isPending) {
    return <StatusMessage>Loading…</StatusMessage>
  }
  if (title.isError) {
    if (title.error instanceof ApiError && title.error.status === 404) {
      return (
        <div className="py-24 text-center">
          <p className="text-lg text-slate-200">We couldn't find that title.</p>
          <Link to="/" className="mt-4 inline-block text-sm text-sky-300 hover:text-sky-200">
            Back to home
          </Link>
        </div>
      )
    }
    return <StatusMessage onRetry={() => void title.refetch()}>{errorMessage(title.error)}</StatusMessage>
  }
  return <TitleContent title={title.data} />
}

function TitleContent({ title }: { title: TitleDetails }) {
  const backdrop = backdropUrl(title.backdropPath)

  return (
    <article className="flex flex-col gap-10">
      {/* Hero: dimmed backdrop behind poster and headline facts. */}
      <header className="relative -mx-4 -mt-12 overflow-hidden px-4 pt-12 pb-8 sm:rounded-b-3xl">
        {backdrop && (
          <>
            <img src={backdrop} alt="" aria-hidden="true" className="absolute inset-0 size-full object-cover opacity-30" />
            <div className="absolute inset-0 bg-linear-to-t from-ink via-ink/80 to-ink/40" />
          </>
        )}
        <div className="relative flex flex-col gap-6 sm:flex-row sm:items-end">
          <PosterImage path={title.posterPath} title={title.title} size="w500" className="w-40 shrink-0 shadow-2xl shadow-black/60 sm:w-52" />
          <div className="flex flex-col gap-3">
            <div>
              <MediaBadge mediaType={title.mediaType} anime={title.anime} />
            </div>
            <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">
              {title.title}
              {yearOf(title.releaseDate) && (
                <span className="ml-2 font-normal text-slate-400">({yearOf(title.releaseDate)})</span>
              )}
            </h1>
            {title.tagline && <p className="text-slate-300 italic">{title.tagline}</p>}
            {title.genres.length > 0 && (
              <ul aria-label="Genres" className="flex flex-wrap gap-2">
                {title.genres.map((genre) => (
                  <li key={genre.id} className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs text-slate-300">
                    {genre.name}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </header>

      <StaleNotice fetchedAt={title.fetchedAt} />

      <div className="grid gap-10 lg:grid-cols-[2fr_1fr]">
        <section aria-labelledby="overview-heading" className="flex flex-col gap-3">
          <h2 id="overview-heading" className="text-lg font-semibold">
            Overview
          </h2>
          <p className="leading-relaxed text-slate-300">{title.overview || 'No overview available.'}</p>
        </section>
        <Facts title={title} />
      </div>

      {title.mediaType === 'TV' && title.seasons.length > 0 && (
        <SeasonList tvTmdbId={title.tmdbId} seasons={title.seasons} />
      )}
    </article>
  )
}

/** Key facts. A row only appears when we actually know the value. */
function Facts({ title }: { title: TitleDetails }) {
  const isMovie = title.mediaType === 'MOVIE'
  const rows: [string, string | null][] = [
    // TMDB's main date is often the premiere, not the UK date, so it is labelled plainly.
    [isMovie ? 'Release date' : 'First aired', formatDate(title.releaseDate)],
    ['UK cinema release', isMovie ? formatDate(title.ukReleaseDate) : null],
    ['Runtime', isMovie ? formatRuntime(title.runtimeMinutes) : null],
    ['Status', title.status],
    ['Original language', languageName(title.originalLanguage)],
    ['Original title', title.originalTitle && title.originalTitle !== title.title ? title.originalTitle : null],
  ]
  const known = rows.filter((row): row is [string, string] => row[1] !== null)

  return (
    <section aria-labelledby="facts-heading" className="flex flex-col gap-3">
      <h2 id="facts-heading" className="text-lg font-semibold">
        Details
      </h2>
      <dl className="divide-y divide-white/5 rounded-xl border border-white/10 bg-white/[0.03]">
        {known.map(([label, value]) => (
          <div key={label} className="flex justify-between gap-4 px-4 py-3 text-sm">
            <dt className="text-slate-400">{label}</dt>
            <dd className="text-right text-slate-100">{value}</dd>
          </div>
        ))}
      </dl>
    </section>
  )
}

/** Shown only when the saved copy is old, e.g. served because TMDB was down. */
function StaleNotice({ fetchedAt }: { fetchedAt: string }) {
  // Read the clock once, when the notice first appears, so re-renders stay predictable.
  const [now] = useState(() => Date.now())
  const ageDays = (now - new Date(fetchedAt).getTime()) / MS_PER_DAY
  if (!(ageDays > STALE_AFTER_DAYS)) {
    return null
  }
  return (
    <p role="note" className="rounded-lg border border-white/10 bg-white/5 px-4 py-3 text-sm text-slate-300">
      Details last updated {formatDate(fetchedAt.slice(0, 10))}. TMDB may have newer information.
    </p>
  )
}
