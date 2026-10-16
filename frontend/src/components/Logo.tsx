import { Link } from 'react-router'

/**
 * Logo: a dark tile with a white "play" mark and a thin sky-blue accent line,
 * then the wordmark in white and slate. Deliberately quiet.
 */
export function Logo() {
  return (
    <Link to="/" className="flex items-center gap-2.5" aria-label="WatchVault home">
      <span className="relative grid size-8 place-items-center overflow-hidden rounded-lg bg-slate-900 ring-1 ring-white/15">
        <svg viewBox="0 0 24 24" className="size-4 fill-white" aria-hidden="true">
          <path d="M8 5.5v13l10.5-6.5z" />
        </svg>
        <span className="absolute inset-x-0 bottom-0 h-0.5 bg-sky-400" />
      </span>
      <span className="text-xl font-semibold tracking-tight">
        Watch<span className="text-slate-400">Vault</span>
      </span>
    </Link>
  )
}
