import { useInfiniteQuery, useQuery } from '@tanstack/react-query'

import { titlesApi, type MediaPath } from '../api/titles'

const TEN_MINUTES_MS = 10 * 60 * 1000

/** Search, one page at a time; "Load more" asks for the next page. */
export function useSearch(query: string) {
  return useInfiniteQuery({
    queryKey: ['search', query.toLowerCase()],
    queryFn: ({ pageParam }) => titlesApi.search(query, pageParam),
    initialPageParam: 1,
    getNextPageParam: (lastPage) => (lastPage.page < lastPage.totalPages ? lastPage.page + 1 : undefined),
    enabled: query.length > 0,
    staleTime: TEN_MINUTES_MS,
  })
}

export function useTitle(media: MediaPath, tmdbId: number) {
  return useQuery({
    queryKey: ['title', media, tmdbId],
    queryFn: () => titlesApi.details(media, tmdbId),
    staleTime: TEN_MINUTES_MS,
  })
}

export function useWatchProviders(media: MediaPath, tmdbId: number) {
  return useQuery({
    queryKey: ['providers', media, tmdbId],
    queryFn: () => titlesApi.providers(media, tmdbId),
    staleTime: TEN_MINUTES_MS,
  })
}

/** Only fetched once the season is opened (enabled). */
export function useSeason(tvTmdbId: number, seasonNumber: number, enabled: boolean) {
  return useQuery({
    queryKey: ['season', tvTmdbId, seasonNumber],
    queryFn: () => titlesApi.season(tvTmdbId, seasonNumber),
    enabled,
    staleTime: TEN_MINUTES_MS,
  })
}
