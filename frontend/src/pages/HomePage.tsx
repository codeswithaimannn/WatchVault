import { Link } from 'react-router'

import { useCurrentUser } from '../auth/authHooks'

/** Example searches to get started: one film, one series, one anime. */
const SUGGESTED_SEARCHES = ['Dune', 'Severance', 'Spirited Away']

/** Home. The library, "continue watching" and recommendations replace this in later milestones. */
export function HomePage() {
  const { data: user } = useCurrentUser()

  return (
    <section className="flex flex-col gap-8">
      <div>
        <p className="text-brand-gradient text-sm font-semibold tracking-widest uppercase">Your vault</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight sm:text-4xl">Welcome, {user?.displayName}</h1>
      </div>

      {/* Hairline gradient border: a 1px gradient frame around a dark panel. */}
      <div className="rounded-2xl bg-linear-to-br from-sky-500/25 via-white/5 to-teal-500/20 p-px">
        <div className="rounded-2xl bg-ink/90 px-6 py-14 text-center">
          <div className="bg-brand-gradient mx-auto grid size-12 place-items-center rounded-xl opacity-90 shadow-lg shadow-sky-900/30">
            <svg viewBox="0 0 24 24" className="size-6 fill-white" aria-hidden="true">
              <path d="M8 5.5v13l10.5-6.5z" />
            </svg>
          </div>
          <p className="mt-5 text-lg font-medium text-slate-200">Your library is empty for now.</p>
          <p className="mt-1.5 text-sm text-slate-400">
            Use the search bar above to find films, series and anime. Adding them to your library comes next.
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
    </section>
  )
}
