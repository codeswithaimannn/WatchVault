import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'

import { errorMessage } from '../api/client'
import type { LibraryEntry, LibraryStatus } from '../api/library'
import { toPath } from '../api/titles'
import { formatDate } from '../lib/format'
import { useRemoveLibraryEntry, useSaveLibraryEntry } from './libraryHooks'
import { LIBRARY_STATUSES, MAX_NOTES_LENGTH, MAX_RATING, MIN_RATING } from './statuses'

const RATINGS = Array.from({ length: MAX_RATING - MIN_RATING + 1 }, (_, i) => MIN_RATING + i)

const choiceClass =
  'rounded-lg border border-white/10 px-3 py-1.5 text-sm text-slate-300 transition hover:bg-white/5 aria-pressed:border-sky-400/60 aria-pressed:bg-sky-500/15 aria-pressed:text-white disabled:opacity-60'

interface LibraryEditorProps {
  entry: LibraryEntry
  onClose: () => void
}

/**
 * Edit panel (a modal dialog) for one library entry. Choices are buttons rather than native
 * dropdowns: they look the same in every browser and stay readable on the dark theme.
 * Changes are kept locally until Save, so Cancel really cancels.
 */
export function LibraryEditor({ entry, onClose }: LibraryEditorProps) {
  const media = toPath(entry.mediaType)
  const save = useSaveLibraryEntry(media, entry.tmdbId)
  const remove = useRemoveLibraryEntry(media, entry.tmdbId)
  const [status, setStatus] = useState<LibraryStatus>(entry.status)
  const [rating, setRating] = useState<number | null>(entry.rating)
  const [notes, setNotes] = useState(entry.notes ?? '')
  const dialogRef = useRef<HTMLDivElement>(null)
  const busy = save.isPending || remove.isPending
  const name = entry.title?.title ?? 'This title'

  // Move keyboard focus into the dialog when it opens.
  useEffect(() => {
    dialogRef.current?.focus()
  }, [])

  const handleSave = () =>
    save.mutate({ status, rating, notes: notes.trim() ? notes : null }, { onSuccess: onClose })
  const handleRemove = () => remove.mutate(undefined, { onSuccess: onClose })

  return (
    <div className="fixed inset-0 z-30 grid place-items-center bg-black/60 p-4 backdrop-blur-sm" onClick={onClose}>
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="editor-title"
        tabIndex={-1}
        onClick={(event) => event.stopPropagation()}
        onKeyDown={(event) => event.key === 'Escape' && onClose()}
        className="flex max-h-full w-full max-w-lg flex-col gap-5 overflow-y-auto rounded-2xl border border-white/10 bg-slate-950 p-6 shadow-2xl shadow-black/60 focus:outline-none"
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 id="editor-title" className="text-xl font-semibold">
              {name}
            </h2>
            <Link to={`/title/${media}/${entry.tmdbId}`} className="text-sm text-sky-300 hover:text-sky-200">
              Open title page
            </Link>
          </div>
          <button type="button" onClick={onClose} aria-label="Close" className="rounded-lg px-2 py-1 text-slate-400 hover:bg-white/5 hover:text-white">
            ✕
          </button>
        </div>

        <fieldset className="flex flex-col gap-2">
          <legend className="mb-2 text-sm text-slate-300">Status</legend>
          <div className="flex flex-wrap gap-2">
            {LIBRARY_STATUSES.map((option) => (
              <button
                key={option.value}
                type="button"
                aria-pressed={status === option.value}
                disabled={busy}
                onClick={() => setStatus(option.value)}
                className={choiceClass}
              >
                {option.label}
              </button>
            ))}
          </div>
          {status === 'COMPLETED' && entry.completedOn && entry.status === 'COMPLETED' && (
            <p className="text-xs text-slate-500">Completed on {formatDate(entry.completedOn)}</p>
          )}
        </fieldset>

        <fieldset className="flex flex-col gap-2">
          <legend className="mb-2 text-sm text-slate-300">Your rating</legend>
          <div className="flex flex-wrap gap-1.5">
            {RATINGS.map((value) => (
              <button
                key={value}
                type="button"
                aria-pressed={rating === value}
                aria-label={`${value} out of 10`}
                disabled={busy}
                onClick={() => setRating(value)}
                className={`${choiceClass} w-10 px-0 tabular-nums`}
              >
                {value}
              </button>
            ))}
            <button type="button" disabled={busy || rating === null} onClick={() => setRating(null)} className={choiceClass}>
              Clear
            </button>
          </div>
        </fieldset>

        <div className="flex flex-col gap-1.5 text-sm">
          <label htmlFor="editor-notes" className="text-slate-300">
            Private notes
          </label>
          <textarea
            id="editor-notes"
            value={notes}
            maxLength={MAX_NOTES_LENGTH}
            rows={3}
            disabled={busy}
            onChange={(event) => setNotes(event.target.value)}
            className="w-full resize-y rounded-lg border border-white/10 bg-white/5 px-3 py-2 text-slate-100 focus:border-sky-400/60 focus:ring-2 focus:ring-sky-500/25 focus:outline-none"
          />
          <span className="self-end text-xs text-slate-500">
            {notes.length}/{MAX_NOTES_LENGTH}
          </span>
        </div>

        {(save.isError || remove.isError) && (
          <p role="alert" className="text-sm text-red-300">
            {errorMessage(save.error ?? remove.error)}
          </p>
        )}

        <div className="flex items-center justify-between gap-3">
          <button type="button" disabled={busy} onClick={handleRemove} className="text-sm text-slate-400 hover:text-red-300 disabled:opacity-60">
            Remove from library
          </button>
          <div className="flex gap-2">
            <button type="button" onClick={onClose} className="rounded-lg border border-white/10 px-4 py-2 text-sm text-slate-300 hover:bg-white/5">
              Cancel
            </button>
            <button
              type="button"
              disabled={busy}
              onClick={handleSave}
              className="bg-brand-gradient rounded-lg px-4 py-2 text-sm font-semibold text-white transition hover:brightness-110 disabled:opacity-60"
            >
              {save.isPending ? 'Saving…' : 'Save'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
