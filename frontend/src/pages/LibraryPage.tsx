import { Link, useSearchParams } from 'react-router'

import { errorMessage } from '../api/client'
import { StatusMessage } from '../components/StatusMessage'
import { LibraryCard } from '../library/LibraryCard'
import {
  countByStatus,
  filterAndSort,
  type SortOrder,
  type StatusFilter,
  type TypeFilter,
} from '../library/filters'
import { useLibrary } from '../library/libraryHooks'
import { LIBRARY_STATUSES } from '../library/statuses'

const STATUS_TABS: { value: StatusFilter; label: string }[] = [{ value: 'ALL', label: 'All' }, ...LIBRARY_STATUSES]
const TYPE_OPTIONS: { value: TypeFilter; label: string }[] = [
  { value: 'ALL', label: 'All' },
  { value: 'MOVIE', label: 'Movies' },
  { value: 'TV', label: 'TV' },
  { value: 'ANIME', label: 'Anime' },
]
const SORT_OPTIONS: { value: SortOrder; label: string }[] = [
  { value: 'UPDATED', label: 'Recently updated' },
  { value: 'TITLE', label: 'Title A–Z' },
  { value: 'RATING', label: 'Your rating' },
]

/** Reads a URL parameter, falling back to the default if it's missing or not an allowed value. */
function pick<T extends string>(value: string | null, allowed: { value: T }[], fallback: T): T {
  return allowed.some((option) => option.value === value) ? (value as T) : fallback
}

export function LibraryPage() {
  const library = useLibrary()
  const [params, setParams] = useSearchParams()
  const filters = {
    status: pick(params.get('status'), STATUS_TABS, 'ALL'),
    type: pick(params.get('type'), TYPE_OPTIONS, 'ALL'),
    sort: pick(params.get('sort'), SORT_OPTIONS, 'UPDATED'),
  }

  // Filters live in the URL, so refresh and Back keep them. Defaults are left out to keep URLs short.
  const setFilter = (name: 'status' | 'type' | 'sort', value: string, defaultValue: string) => {
    const next = new URLSearchParams(params)
    if (value === defaultValue) next.delete(name)
    else next.set(name, value)
    setParams(next, { replace: true })
  }

  if (library.isPending) {
    return <StatusMessage>Loading your library…</StatusMessage>
  }
  if (library.isError) {
    return <StatusMessage onRetry={() => void library.refetch()}>{errorMessage(library.error)}</StatusMessage>
  }

  const entries = library.data
  const counts = countByStatus(entries, filters.type)
  const shown = filterAndSort(entries, filters)

  return (
    <section className="flex flex-col gap-6">
      <h1 className="text-3xl font-semibold tracking-tight">Your library</h1>

      {entries.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-white/10 px-6 py-16 text-center">
          <p className="text-lg text-slate-200">Your library is empty.</p>
          <p className="mt-2 text-sm text-slate-400">
            Search for a film or series and choose a status to add it.{' '}
            <Link to="/" className="text-sky-300 hover:text-sky-200">
              Go to home
            </Link>
          </p>
        </div>
      ) : (
        <>
          <nav aria-label="Filter by status" className="flex flex-wrap gap-2">
            {STATUS_TABS.map((tab) => (
              <button
                key={tab.value}
                type="button"
                aria-pressed={filters.status === tab.value}
                onClick={() => setFilter('status', tab.value, 'ALL')}
                className="rounded-full border border-white/10 px-3 py-1 text-sm text-slate-300 transition hover:bg-white/5 aria-pressed:border-sky-400/50 aria-pressed:bg-sky-500/10 aria-pressed:text-white"
              >
                {tab.label} <span className="text-slate-500 tabular-nums">{counts[tab.value]}</span>
              </button>
            ))}
          </nav>

          <div className="flex flex-wrap items-center justify-between gap-3">
            <div role="group" aria-label="Filter by type" className="flex overflow-hidden rounded-lg border border-white/10">
              {TYPE_OPTIONS.map((option) => (
                <button
                  key={option.value}
                  type="button"
                  aria-pressed={filters.type === option.value}
                  onClick={() => setFilter('type', option.value, 'ALL')}
                  className="px-3 py-1.5 text-sm text-slate-400 transition hover:bg-white/5 aria-pressed:bg-white/10 aria-pressed:text-white"
                >
                  {option.label}
                </button>
              ))}
            </div>
            <label className="flex items-center gap-2 text-sm text-slate-400">
              Sort by
              <select
                value={filters.sort}
                onChange={(event) => setFilter('sort', event.target.value, 'UPDATED')}
                className="rounded-lg border border-white/10 bg-white/5 px-2 py-1.5 text-sm text-slate-100 focus:border-sky-400/60 focus:outline-none"
              >
                {SORT_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          {shown.length === 0 ? (
            <StatusMessage>Nothing here with these filters.</StatusMessage>
          ) : (
            <ul className="grid grid-cols-2 gap-x-3 gap-y-6 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6">
              {shown.map((entry) => (
                <li key={`${entry.mediaType}-${entry.tmdbId}`}>
                  <LibraryCard entry={entry} />
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </section>
  )
}
