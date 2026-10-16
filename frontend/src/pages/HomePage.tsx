import { Link } from 'react-router'

import type { LibraryEntry } from '../api/library'
import { errorMessage } from '../api/client'
import { useCurrentUser } from '../auth/authHooks'
import { LibraryCard } from '../library/LibraryCard'
import { editHref } from '../library/editKey'
import { countByStatus } from '../library/filters'
import { useLibrary } from '../library/libraryHooks'
import { LIBRARY_STATUSES } from '../library/statuses'

/** Example searches to get started: one film, one series, one anime. */
const SUGGESTED_SEARCHES = ['Dune', 'Severance', 'Spirited Away']
const CONTINUE_WATCHING_LIMIT = 6

export function HomePage() {
  const { data: user } = useCurrentUser()
  const library = useLibrary()

  return (
    <section className="flex flex-col gap-10">
      <div>
        <p className="text-brand-gradient text-sm font-semibold tracking-widest uppercase">Your vault</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight sm:text-4xl">Welcome, {user?.displayName}</h1>
      </div>

      {library.isPending && <p className="text-sm text-slate-400">Loading your library…</p>}
      {library.isError && <p className="text-sm text-slate-400">{errorMessage(library.error)}</p>}
      {library.isSuccess &&
        (library.data.length === 0 ? <EmptyLibrary /> : <LibrarySummary entries={library.data} />)}
    </section>
  )
}

function LibrarySummary({ entries }: { entries: LibraryEntry[] }) {
  const counts = countByStatus(entries, 'ALL')
  // The list arrives most recently updated first, so this keeps that order.
  const watching = entries.filter((entry) => entry.status === 'WATCHING').slice(0, CONTINUE_WATCHING_LIMIT)

  return (
    <>
      <nav aria-label="Library by status" className="grid grid-cols-2 gap-3 sm:grid-cols-5">
        {LIBRARY_STATUSES.map((status) => (
          <Link
            key={status.value}
            to={`/library?status=${status.value}`}
            className="rounded-xl border border-white/10 bg-white/[0.03] px-4 py-3 transition hover:border-sky-400/30 hover:bg-white/5"
          >
            <span className="block text-2xl font-semibold text-slate-100 tabular-nums">{counts[status.value]}</span>
            <span className="text-xs text-slate-400">{status.label}</span>
          </Link>
        ))}
      </nav>

      <section aria-labelledby="continue-heading" className="flex flex-col gap-4">
        <div className="flex items-baseline justify-between">
          <h2 id="continue-heading" className="text-lg font-semibold">
            Continue watching
          </h2>
          <Link to="/library" className="text-sm text-sky-300 hover:text-sky-200">
            Open library
          </Link>
        </div>
        {watching.length === 0 ? (
          <p className="text-sm text-slate-400">
            Nothing in progress.{' '}
            <Link to="/library?status=PLAN_TO_WATCH" className="text-sky-300 hover:text-sky-200">
              Pick something from Plan to watch
            </Link>
            .
          </p>
        ) : (
          <ul className="grid grid-cols-2 gap-x-3 gap-y-6 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6">
            {watching.map((entry) => (
              <li key={`${entry.mediaType}-${entry.tmdbId}`}>
                <LibraryCard entry={entry} href={editHref(entry.mediaType, entry.tmdbId)} />
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  )
}

function EmptyLibrary() {
  return (
    // Hairline gradient border: a 1px gradient frame around a dark panel.
    <div className="rounded-2xl bg-linear-to-br from-sky-500/25 via-white/5 to-teal-500/20 p-px">
      <div className="rounded-2xl bg-ink/90 px-6 py-14 text-center">
        <div className="bg-brand-gradient mx-auto grid size-12 place-items-center rounded-xl opacity-90 shadow-lg shadow-sky-900/30">
          <svg viewBox="0 0 24 24" className="size-6 fill-white" aria-hidden="true">
            <path d="M8 5.5v13l10.5-6.5z" />
          </svg>
        </div>
        <p className="mt-5 text-lg font-medium text-slate-200">Your library is empty for now.</p>
        <p className="mt-1.5 text-sm text-slate-400">
          Search for a film, series or anime, then choose a status on its page to add it.
        </p>
        <div className="mt-6 flex flex-wrap justify-center gap-2">
          <span className="self-center text-xs text-slate-500">Try:</span>
          {SUGGESTED_SEARCHES.map((query) => (
            <Link
              key={query}
              to={`/search?q=${encodeURIComponent(query)}`}
              className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-sm text-slate-300 transition hover:border-sky-400/40 hover:text-white"
            >
              {query}
            </Link>
          ))}
        </div>
      </div>
    </div>
  )
}
