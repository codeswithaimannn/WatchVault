import type { LibraryStatus } from '../api/library'

/** Display order and labels for library statuses. */
export const LIBRARY_STATUSES: { value: LibraryStatus; label: string }[] = [
  { value: 'WATCHING', label: 'Watching' },
  { value: 'PLAN_TO_WATCH', label: 'Plan to watch' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'ON_HOLD', label: 'On hold' },
  { value: 'DROPPED', label: 'Dropped' },
]

export function statusLabel(status: LibraryStatus): string {
  return LIBRARY_STATUSES.find((s) => s.value === status)?.label ?? status
}

export const MIN_RATING = 1
export const MAX_RATING = 10
export const MAX_NOTES_LENGTH = 1000
