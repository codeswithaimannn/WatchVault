import { useState, type ReactNode } from 'react'

import { errorMessage } from '../api/client'
import type { LibraryEntry, LibraryStatus, SaveLibraryEntry } from '../api/library'
import type { MediaPath } from '../api/titles'
import { formatDate } from '../lib/format'
import { useLibraryEntry, useRemoveLibraryEntry, useSaveLibraryEntry } from './libraryHooks'
import { LIBRARY_STATUSES, MAX_NOTES_LENGTH, MAX_RATING, MIN_RATING } from './statuses'

const RATINGS = Array.from({ length: MAX_RATING - MIN_RATING + 1 }, (_, i) => MIN_RATING + i)
const NOT_IN_LIBRARY = ''

const fieldClass =
  'w-full rounded-lg border border-white/10 bg-white/5 px-3 py-2 text-sm text-slate-100 focus:border-sky-400/60 focus:ring-2 focus:ring-sky-500/25 focus:outline-none disabled:opacity-60'

/** "Your library" on a title page: add, change status/rating/notes, or remove. */
export function LibraryPanel({ media, tmdbId }: { media: MediaPath; tmdbId: number }) {
  const entry = useLibraryEntry(media, tmdbId)
  const save = useSaveLibraryEntry(media, tmdbId)
  const remove = useRemoveLibraryEntry(media, tmdbId)
  const busy = save.isPending || remove.isPending

  if (entry.isPending) {
    return <Panel>Loading your library…</Panel>
  }
  if (entry.isError) {
    return <Panel>{errorMessage(entry.error)}</Panel>
  }

  const current = entry.data
  // Every save sends the whole entry; this fills in whatever isn't changing.
  const saveWith = (changes: Partial<SaveLibraryEntry>) =>
    save.mutate({
      status: current?.status ?? 'PLAN_TO_WATCH',
      rating: current?.rating ?? null,
      notes: current?.notes ?? null,
      ...changes,
    })

  return (
    <Panel>
      <div className="flex flex-col gap-4">
        <label className="flex flex-col gap-1.5 text-sm">
          <span className="text-slate-300">Status</span>
          <select
            value={current?.status ?? NOT_IN_LIBRARY}
            disabled={busy}
            onChange={(event) => saveWith({ status: event.target.value as LibraryStatus })}
            className={fieldClass}
          >
            {!current && <option value={NOT_IN_LIBRARY}>Add to library…</option>}
            {LIBRARY_STATUSES.map((status) => (
              <option key={status.value} value={status.value}>
                {status.label}
              </option>
            ))}
          </select>
        </label>

        {current && (
          <>
            {current.completedOn && (
              <p className="text-xs text-slate-400">Completed on {formatDate(current.completedOn)}</p>
            )}
            <label className="flex flex-col gap-1.5 text-sm">
              <span className="text-slate-300">Your rating</span>
              <select
                value={current.rating ?? ''}
                disabled={busy}
                onChange={(event) => saveWith({ rating: event.target.value ? Number(event.target.value) : null })}
                className={fieldClass}
              >
                <option value="">Not rated</option>
                {RATINGS.map((rating) => (
                  <option key={rating} value={rating}>
                    {rating}/10
                  </option>
                ))}
              </select>
            </label>
            {/* key: start fresh whenever the saved notes change */}
            <NotesForm key={current.updatedAt} entry={current} busy={busy} onSave={(notes) => saveWith({ notes })} />
            <button
              type="button"
              disabled={busy}
              onClick={() => remove.mutate()}
              className="self-start text-sm text-slate-400 underline-offset-4 hover:text-red-300 hover:underline disabled:opacity-60"
            >
              Remove from library
            </button>
          </>
        )}

        {busy && <p className="text-xs text-slate-400">Saving…</p>}
        {(save.isError || remove.isError) && (
          <p role="alert" className="text-sm text-red-300">
            {errorMessage(save.error ?? remove.error)}
          </p>
        )}
      </div>
    </Panel>
  )
}

function NotesForm({ entry, busy, onSave }: { entry: LibraryEntry; busy: boolean; onSave: (notes: string | null) => void }) {
  const [notes, setNotes] = useState(entry.notes ?? '')
  const unchanged = notes === (entry.notes ?? '')

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault()
        onSave(notes.trim() ? notes : null)
      }}
      className="flex flex-col gap-1.5 text-sm"
    >
      <label htmlFor="library-notes" className="text-slate-300">
        Private notes
      </label>
      <textarea
        id="library-notes"
        value={notes}
        maxLength={MAX_NOTES_LENGTH}
        rows={3}
        onChange={(event) => setNotes(event.target.value)}
        className={`${fieldClass} resize-y`}
      />
      <div className="flex items-center justify-between">
        <span className="text-xs text-slate-500">
          {notes.length}/{MAX_NOTES_LENGTH}
        </span>
        <button
          type="submit"
          disabled={busy || unchanged}
          className="rounded-lg border border-white/10 bg-white/5 px-3 py-1.5 text-sm text-slate-200 transition hover:bg-white/10 disabled:opacity-50"
        >
          Save notes
        </button>
      </div>
    </form>
  )
}

function Panel({ children }: { children: ReactNode }) {
  return (
    <section aria-labelledby="library-heading" className="flex flex-col gap-3">
      <h2 id="library-heading" className="text-lg font-semibold">
        Your library
      </h2>
      <div className="rounded-xl border border-white/10 bg-white/[0.03] p-4 text-sm text-slate-400">{children}</div>
    </section>
  )
}
