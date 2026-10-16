import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { libraryApi, type SaveLibraryEntry } from '../api/library'
import type { MediaPath } from '../api/titles'

/** All library queries start with this key, so one invalidation refreshes them all. */
const LIBRARY_KEY = ['library'] as const

export function useLibrary() {
  return useQuery({ queryKey: LIBRARY_KEY, queryFn: libraryApi.list })
}

export function useLibraryEntry(media: MediaPath, tmdbId: number) {
  return useQuery({
    queryKey: [...LIBRARY_KEY, media, tmdbId],
    queryFn: () => libraryApi.get(media, tmdbId),
  })
}

export function useSaveLibraryEntry(media: MediaPath, tmdbId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (entry: SaveLibraryEntry) => libraryApi.save(media, tmdbId, entry),
    onSuccess: (saved) => {
      queryClient.setQueryData([...LIBRARY_KEY, media, tmdbId], saved)
      void queryClient.invalidateQueries({ queryKey: LIBRARY_KEY, exact: true }) // the full list
    },
  })
}

export function useRemoveLibraryEntry(media: MediaPath, tmdbId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => libraryApi.remove(media, tmdbId),
    onSuccess: () => {
      queryClient.setQueryData([...LIBRARY_KEY, media, tmdbId], null)
      void queryClient.invalidateQueries({ queryKey: LIBRARY_KEY, exact: true })
    },
  })
}
