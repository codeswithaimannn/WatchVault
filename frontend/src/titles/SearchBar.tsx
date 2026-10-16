import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'

interface SearchBarProps {
  /** The current query from the URL. Layout re-mounts this component (key) when it changes. */
  initialQuery: string
}

/** Header search. Submitting goes to /search?q=..., so the URL holds the query (Back, refresh, sharing). */
export function SearchBar({ initialQuery }: SearchBarProps) {
  const [query, setQuery] = useState(initialQuery)
  const navigate = useNavigate()

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    const trimmed = query.trim()
    if (trimmed) {
      navigate(`/search?q=${encodeURIComponent(trimmed)}`)
    }
  }

  return (
    <form role="search" onSubmit={handleSubmit} className="relative w-full max-w-md">
      <svg
        viewBox="0 0 20 20"
        aria-hidden="true"
        className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 fill-slate-500"
      >
        <path d="M8.5 3a5.5 5.5 0 0 1 4.383 8.823l3.647 3.647a.75.75 0 1 1-1.06 1.06l-3.647-3.646A5.5 5.5 0 1 1 8.5 3Zm0 1.5a4 4 0 1 0 0 8 4 4 0 0 0 0-8Z" />
      </svg>
      <input
        type="search"
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        placeholder="Search films, series and anime"
        aria-label="Search films, series and anime"
        maxLength={100}
        className="w-full rounded-lg border border-white/10 bg-white/5 py-2 pr-3 pl-9 text-sm text-slate-100 transition placeholder:text-slate-500 focus:border-sky-400/60 focus:bg-white/[0.07] focus:ring-2 focus:ring-sky-500/25 focus:outline-none"
      />
    </form>
  )
}
