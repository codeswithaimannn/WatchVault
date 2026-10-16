import type { ReactNode } from 'react'

interface StatusMessageProps {
  children: ReactNode
  onRetry?: () => void
}

/** Centred loading or error text, with an optional "Try again" button. */
export function StatusMessage({ children, onRetry }: StatusMessageProps) {
  return (
    <div role="status" className="flex flex-col items-center gap-4 py-24 text-slate-400">
      <p>{children}</p>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="rounded-lg border border-white/10 bg-white/5 px-4 py-2 text-sm text-slate-200 transition hover:bg-white/10"
        >
          Try again
        </button>
      )}
    </div>
  )
}
