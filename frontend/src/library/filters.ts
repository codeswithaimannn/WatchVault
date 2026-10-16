import type { LibraryEntry, LibraryStatus } from '../api/library'

export type StatusFilter = LibraryStatus | 'ALL'
/** "Anime" overlaps the others: an anime series is also TV. */
export type TypeFilter = 'ALL' | 'MOVIE' | 'TV' | 'ANIME'
export type SortOrder = 'UPDATED' | 'TITLE' | 'RATING'

export interface LibraryFilters {
  status: StatusFilter
  type: TypeFilter
  sort: SortOrder
}

function matchesType(entry: LibraryEntry, type: TypeFilter): boolean {
  switch (type) {
    case 'ALL':
      return true
    case 'ANIME':
      return entry.title?.anime === true
    default:
      return entry.mediaType === type
  }
}

const byUpdatedNewestFirst = (a: LibraryEntry, b: LibraryEntry) => b.updatedAt.localeCompare(a.updatedAt)

/** Titles A-Z (British English rules); entries without title info go last. */
function byTitle(a: LibraryEntry, b: LibraryEntry): number {
  if (!a.title || !b.title) return a.title ? -1 : b.title ? 1 : 0
  return a.title.title.localeCompare(b.title.title, 'en-GB', { sensitivity: 'base' })
}

/** Highest rating first; unrated go last; ties by most recently updated. */
function byRating(a: LibraryEntry, b: LibraryEntry): number {
  const difference = (b.rating ?? 0) - (a.rating ?? 0)
  return difference !== 0 ? difference : byUpdatedNewestFirst(a, b)
}

const SORTERS: Record<SortOrder, (a: LibraryEntry, b: LibraryEntry) => number> = {
  UPDATED: byUpdatedNewestFirst,
  TITLE: byTitle,
  RATING: byRating,
}

/** Applies the type filter, then the status filter, then sorts. Never changes the input. */
export function filterAndSort(entries: LibraryEntry[], filters: LibraryFilters): LibraryEntry[] {
  return entries
    .filter((entry) => matchesType(entry, filters.type))
    .filter((entry) => filters.status === 'ALL' || entry.status === filters.status)
    .toSorted(SORTERS[filters.sort])
}

/** How many entries have each status (after the type filter), for the tab counts. */
export function countByStatus(entries: LibraryEntry[], type: TypeFilter): Record<StatusFilter, number> {
  const counts: Record<StatusFilter, number> = { ALL: 0, PLAN_TO_WATCH: 0, WATCHING: 0, COMPLETED: 0, ON_HOLD: 0, DROPPED: 0 }
  for (const entry of entries) {
    if (matchesType(entry, type)) {
      counts.ALL++
      counts[entry.status]++
    }
  }
  return counts
}
