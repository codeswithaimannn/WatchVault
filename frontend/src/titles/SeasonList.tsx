import { useState } from 'react'

import { errorMessage } from '../api/client'
import type { Episode, SeasonSummary } from '../api/titles'
import { formatDate, yearOf } from '../lib/format'
import { useSeason } from './titleHooks'

/** TMDB puts specials in season 0. */
const SPECIALS_SEASON = 0

/** Regular seasons in order, then Specials last. */
function inDisplayOrder(seasons: SeasonSummary[]): SeasonSummary[] {
  const regular = seasons.filter((s) => s.seasonNumber !== SPECIALS_SEASON).sort((a, b) => a.seasonNumber - b.seasonNumber)
  const specials = seasons.filter((s) => s.seasonNumber === SPECIALS_SEASON)
  return [...regular, ...specials]
}

export function SeasonList({ tvTmdbId, seasons }: { tvTmdbId: number; seasons: SeasonSummary[] }) {
  return (
    <section aria-labelledby="seasons-heading" className="flex flex-col gap-3">
      <h2 id="seasons-heading" className="text-lg font-semibold">
        Seasons
      </h2>
      <ul className="flex flex-col gap-2">
        {inDisplayOrder(seasons).map((season) => (
          <li key={season.seasonNumber}>
            <SeasonRow tvTmdbId={tvTmdbId} season={season} />
          </li>
        ))}
      </ul>
    </section>
  )
}

function SeasonRow({ tvTmdbId, season }: { tvTmdbId: number; season: SeasonSummary }) {
  const [open, setOpen] = useState(false)
  const panelId = `season-${season.seasonNumber}-episodes`
  const year = yearOf(season.airDate)

  return (
    <div className="overflow-hidden rounded-xl border border-white/10 bg-white/[0.03]">
      <button
        type="button"
        aria-expanded={open}
        aria-controls={panelId}
        onClick={() => setOpen((isOpen) => !isOpen)}
        className="flex w-full items-center justify-between gap-4 px-4 py-3 text-left transition hover:bg-white/5"
      >
        <span className="font-medium text-slate-100">{season.name}</span>
        <span className="flex items-center gap-3 text-sm text-slate-400">
          {season.episodeCount} {season.episodeCount === 1 ? 'episode' : 'episodes'}
          {year && ` · ${year}`}
          <svg viewBox="0 0 20 20" aria-hidden="true" className={`size-4 fill-slate-400 transition ${open ? 'rotate-180' : ''}`}>
            <path d="M5.22 7.22a.75.75 0 0 1 1.06 0L10 10.94l3.72-3.72a.75.75 0 1 1 1.06 1.06l-4.25 4.25a.75.75 0 0 1-1.06 0L5.22 8.28a.75.75 0 0 1 0-1.06Z" />
          </svg>
        </span>
      </button>
      {open && (
        <div id={panelId} className="border-t border-white/5">
          <SeasonEpisodes tvTmdbId={tvTmdbId} seasonNumber={season.seasonNumber} />
        </div>
      )}
    </div>
  )
}

/** Mounted only while the season is open, so episodes are fetched on demand. */
function SeasonEpisodes({ tvTmdbId, seasonNumber }: { tvTmdbId: number; seasonNumber: number }) {
  const season = useSeason(tvTmdbId, seasonNumber, true)

  if (season.isPending) {
    return <p className="px-4 py-3 text-sm text-slate-400">Loading episodes…</p>
  }
  if (season.isError) {
    return (
      <div className="flex items-center justify-between gap-4 px-4 py-3 text-sm">
        <span className="text-red-300">{errorMessage(season.error)}</span>
        <button type="button" onClick={() => void season.refetch()} className="text-sky-300 hover:text-sky-200">
          Try again
        </button>
      </div>
    )
  }
  if (season.data.episodes.length === 0) {
    return <p className="px-4 py-3 text-sm text-slate-400">No episodes listed yet.</p>
  }
  return (
    <ol className="divide-y divide-white/5">
      {season.data.episodes.map((episode) => (
        <EpisodeRow key={episode.episodeNumber} episode={episode} />
      ))}
    </ol>
  )
}

function EpisodeRow({ episode }: { episode: Episode }) {
  return (
    <li className="flex items-baseline justify-between gap-4 px-4 py-2.5 text-sm">
      <span className="min-w-0">
        <span className="mr-2 text-slate-500 tabular-nums">E{episode.episodeNumber}</span>
        <span className="text-slate-200">{episode.name || 'Untitled episode'}</span>
      </span>
      <span className="flex shrink-0 gap-3 text-slate-400">
        <span>{formatDate(episode.airDate) ?? 'Date not announced'}</span>
        {episode.runtimeMinutes !== null && (
          // "~": TMDB's estimate. Recorded viewing time is a different thing (CLAUDE.md).
          <span title="Estimated runtime" className="tabular-nums">
            ~{episode.runtimeMinutes} min
          </span>
        )}
      </span>
    </li>
  )
}
