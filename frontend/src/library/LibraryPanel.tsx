import type { ReactNode } from 'react'
import { Link } from 'react-router'

import { errorMessage } from '../api/client'
import type { MediaPath } from '../api/titles'
import { editHref } from './editKey'
import { useLibraryEntry, useSaveLibraryEntry } from './libraryHooks'
import { statusLabel } from './statuses'

/**
 * "Your library" on a title page. Deliberately simple: add with one click, then manage
 * everything (status, rating, notes, remove) on the Library page.
 */
export function LibraryPanel({ media, tmdbId }: { media: MediaPath; tmdbId: number }) {
  const entry = useLibraryEntry(media, tmdbId)
  const save = useSaveLibraryEntry(media, tmdbId)

  if (entry.isPending) {
    return <Panel>Loading your library…</Panel>
  }
  if (entry.isError) {
    return <Panel>{errorMessage(entry.error)}</Panel>
  }

  const current = entry.data
  if (!current) {
    return (
      <Panel>
        <button
          type="button"
          disabled={save.isPending}
          onClick={() => save.mutate({ status: 'PLAN_TO_WATCH', rating: null, notes: null })}
          className="bg-brand-gradient w-full rounded-lg px-4 py-2.5 font-semibold text-white shadow-lg shadow-sky-900/30 transition hover:brightness-110 disabled:opacity-60"
        >
          {save.isPending ? 'Adding…' : '+ Add to library'}
        </button>
        <p className="mt-2 text-xs text-slate-500">Added as “Plan to watch”. You can change it in your library.</p>
        {save.isError && (
          <p role="alert" className="mt-2 text-sm text-red-300">
            {errorMessage(save.error)}
          </p>
        )}
      </Panel>
    )
  }

  return (
    <Panel>
      <p className="flex items-center gap-2 text-slate-200">
        <span aria-hidden="true" className="grid size-5 place-items-center rounded-full bg-teal-500/20 text-xs text-teal-300">
          ✓
        </span>
        In your library
      </p>
      <p className="mt-1 text-sm text-slate-400">
        {statusLabel(current.status)}
        {current.rating !== null && ` · ${current.rating}/10`}
      </p>
      <Link
        to={editHref(current.mediaType, current.tmdbId)}
        className="mt-3 inline-block text-sm font-medium text-sky-300 hover:text-sky-200"
      >
        Manage in library →
      </Link>
    </Panel>
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
