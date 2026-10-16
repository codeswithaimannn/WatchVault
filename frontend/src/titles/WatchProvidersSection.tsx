import { errorMessage } from '../api/client'
import type { MediaPath, Provider } from '../api/titles'
import { logoUrl } from '../lib/tmdbImages'
import { useWatchProviders } from './titleHooks'

const JUSTWATCH_URL = 'https://www.justwatch.com/uk'

/**
 * "Where to watch in the UK". The data comes from JustWatch via TMDB, so the JustWatch credit
 * is always shown with it (mandatory, or TMDB may revoke access).
 */
export function WatchProvidersSection({ media, tmdbId }: { media: MediaPath; tmdbId: number }) {
  const providers = useWatchProviders(media, tmdbId)

  return (
    <section aria-labelledby="providers-heading" className="flex flex-col gap-4">
      <h2 id="providers-heading" className="text-lg font-semibold">
        Where to watch in the UK
      </h2>

      {providers.isPending && <p className="text-sm text-slate-400">Checking availability…</p>}

      {providers.isError && (
        <p className="text-sm text-slate-400">
          {errorMessage(providers.error)}{' '}
          <button type="button" onClick={() => void providers.refetch()} className="text-sky-300 hover:text-sky-200">
            Try again
          </button>
        </p>
      )}

      {providers.isSuccess && (
        <>
          {[providers.data.stream, providers.data.rent, providers.data.buy].every((group) => group.length === 0) ? (
            <p className="text-sm text-slate-400">Not currently available to stream, rent or buy in the UK.</p>
          ) : (
            <div className="flex flex-col gap-4">
              <ProviderGroup label="Stream" providers={providers.data.stream} />
              <ProviderGroup label="Rent" providers={providers.data.rent} />
              <ProviderGroup label="Buy" providers={providers.data.buy} />
            </div>
          )}

          <p className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500">
            <span>
              Streaming data from{' '}
              <a href={JUSTWATCH_URL} target="_blank" rel="noopener noreferrer" className="text-slate-300 underline hover:text-white">
                JustWatch
              </a>
            </span>
            {providers.data.link && (
              <a href={providers.data.link} target="_blank" rel="noopener noreferrer" className="text-sky-300 hover:text-sky-200">
                All options on TMDB ↗
              </a>
            )}
          </p>
        </>
      )}
    </section>
  )
}

function ProviderGroup({ label, providers }: { label: string; providers: Provider[] }) {
  if (providers.length === 0) return null

  return (
    <div className="flex flex-col gap-2">
      <h3 className="text-xs font-semibold tracking-widest text-slate-400 uppercase">{label}</h3>
      <ul aria-label={label} className="flex flex-wrap gap-2">
        {providers.map((provider) => {
          const logo = logoUrl(provider.logoPath)
          return (
            <li
              key={provider.id}
              className="flex items-center gap-2 rounded-lg border border-white/10 bg-white/5 py-1 pr-3 pl-1 text-sm text-slate-200"
            >
              {logo ? (
                <img src={logo} alt="" aria-hidden="true" loading="lazy" className="size-7 rounded-md" />
              ) : (
                <span className="size-7 rounded-md bg-slate-700" aria-hidden="true" />
              )}
              {provider.name}
            </li>
          )
        })}
      </ul>
    </div>
  )
}
