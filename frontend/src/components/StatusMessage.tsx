import type { ReactNode } from 'react'

interface StatusMessageProps {
  children: ReactNode
  onRetry?: () => void
}

/** Centred loading or error text, with an optional "Try again" button. */
export function StatusMessage({ children, onRetry }: StatusMessageProps) {
  return (
    <div role="status" className="flex flex-col items-center gap-4 py-24 text-zinc-400">
      <p>{children}</p>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="rounded-md border border-zinc-700 px-4 py-2 text-sm text-zinc-200 hover:bg-zinc-800"
        >
          Try again
        </button>
      )}
    </div>
  )
}
