import { Link } from 'react-router'

/** Gradient "play" mark plus the WatchVault wordmark. Links home. */
export function Logo() {
  return (
    <Link to="/" className="flex items-center gap-2.5" aria-label="WatchVault home">
      <span className="bg-brand-gradient grid size-8 place-items-center rounded-lg shadow-lg shadow-rose-500/20">
        <svg viewBox="0 0 24 24" className="size-4 fill-white" aria-hidden="true">
          <path d="M8 5.5v13l10.5-6.5z" />
        </svg>
      </span>
      <span className="text-xl font-bold tracking-tight">
        Watch<span className="text-brand-gradient">Vault</span>
      </span>
    </Link>
  )
}
