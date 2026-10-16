import { useSearchParams } from 'react-router'

import { errorMessage } from '../api/client'
import { StatusMessage } from '../components/StatusMessage'
import { TitleCard } from '../titles/TitleCard'
import { useSearch } from '../titles/titleHooks'

const SKELETON_CARDS = 12

export function SearchPage() {
  const [searchParams] = useSearchParams()
  const query = (searchParams.get('q') ?? '').trim()
  const search = useSearch(query)

  if (!query) {
    return <StatusMessage>Type a film, series or anime into the search bar above.</StatusMessage>
  }

  const titles = search.data?.pages.flatMap((page) => page.results) ?? []

  return (
    <section className="flex flex-col gap-8">
      <h1 className="text-2xl font-semibold tracking-tight">
        Results for <span className="text-brand-gradient">“{query}”</span>
      </h1>

      {search.isPending && <ResultsSkeleton />}

      {search.isError && (
        <StatusMessage onRetry={() => void search.refetch()}>{errorMessage(search.error)}</StatusMessage>
      )}

      {search.isSuccess && titles.length === 0 && (
        <StatusMessage>No films or series found for “{query}”. Try a different spelling.</StatusMessage>
      )}

      {titles.length > 0 && (
        <ul className="grid grid-cols-2 gap-x-3 gap-y-6 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6">
          {titles.map((title) => (
            <li key={`${title.mediaType}-${title.tmdbId}`}>
              <TitleCard title={title} />
            </li>
          ))}
        </ul>
      )}

      {search.hasNextPage && (
        <div className="flex justify-center">
          <button
            type="button"
            onClick={() => void search.fetchNextPage()}
            disabled={search.isFetchingNextPage}
            className="rounded-lg border border-white/10 bg-white/5 px-5 py-2 text-sm text-slate-200 transition hover:bg-white/10 disabled:opacity-60"
          >
            {search.isFetchingNextPage ? 'Loading…' : 'Load more'}
          </button>
        </div>
      )}
      {search.isFetchNextPageError && (
        <p role="alert" className="text-center text-sm text-red-300">
          {errorMessage(search.error)}
        </p>
      )}
    </section>
  )
}

function ResultsSkeleton() {
  return (
    <ul aria-label="Loading results" className="grid grid-cols-2 gap-x-3 gap-y-6 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6">
      {Array.from({ length: SKELETON_CARDS }, (_, index) => (
        <li key={index} className="flex animate-pulse flex-col gap-2.5 p-1.5">
          <div className="aspect-2/3 rounded-lg bg-white/5" />
          <div className="h-3 w-3/4 rounded bg-white/5" />
        </li>
      ))}
    </ul>
  )
}
