import type { ReactNode } from 'react'

/**
 * About and credits. TMDB's terms require their logo (less prominent than ours) and this exact
 * notice; JustWatch must be credited for streaming data; TVmaze data is CC BY-SA 4.0.
 */
const TMDB_NOTICE = 'This product uses the TMDB API but is not endorsed or certified by TMDB.'

export function AboutPage() {
  return (
    <article className="mx-auto flex max-w-2xl flex-col gap-10">
      <header>
        <p className="text-brand-gradient text-sm font-semibold tracking-widest uppercase">About</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight">About WatchVault</h1>
        <p className="mt-4 leading-relaxed text-slate-300">
          WatchVault is a personal hub for tracking films, series and anime: your library, your progress and what to
          watch next. It is a non-commercial portfolio project.
        </p>
      </header>

      <section aria-labelledby="credits-heading" className="flex flex-col gap-6">
        <h2 id="credits-heading" className="text-lg font-semibold">
          Data sources and credits
        </h2>

        <Credit
          title="TMDB"
          logo={
            // Official, unmodified TMDB logo, kept smaller than our own branding (TMDB terms).
            <img src="/tmdb-logo.svg" alt="The Movie Database (TMDB) logo" className="h-10 w-auto" />
          }
        >
          <p>{TMDB_NOTICE}</p>
          <p>
            Film and TV details, images and genres come from{' '}
            <ExternalLink href="https://www.themoviedb.org">The Movie Database (TMDB)</ExternalLink>.
          </p>
        </Credit>

        <Credit title="JustWatch">
          <p>
            Streaming data from <ExternalLink href="https://www.justwatch.com/uk">JustWatch</ExternalLink>, provided via
            TMDB. Availability changes often; check the service before you watch.
          </p>
        </Credit>

        <Credit title="TVmaze">
          <p>
            Episode schedules from <ExternalLink href="https://www.tvmaze.com">TVmaze</ExternalLink>, licensed under{' '}
            <ExternalLink href="https://creativecommons.org/licenses/by-sa/4.0/">CC BY-SA 4.0</ExternalLink>.
          </p>
        </Credit>
      </section>
    </article>
  )
}

function Credit({ title, logo, children }: { title: string; logo?: ReactNode; children: ReactNode }) {
  return (
    <div className="flex flex-col gap-3 rounded-xl border border-white/10 bg-white/[0.03] p-5 text-sm leading-relaxed text-slate-300">
      <div className="flex items-center justify-between gap-4">
        <h3 className="font-semibold text-slate-100">{title}</h3>
        {logo}
      </div>
      {children}
    </div>
  )
}

function ExternalLink({ href, children }: { href: string; children: ReactNode }) {
  return (
    <a href={href} target="_blank" rel="noopener noreferrer" className="text-sky-300 underline hover:text-sky-200">
      {children}
    </a>
  )
}
