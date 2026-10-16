import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <section className="py-24 text-center">
      <p className="text-brand-gradient text-7xl font-bold">404</p>
      <h1 className="mt-4 text-2xl font-semibold">This scene was cut</h1>
      <p className="mt-2 text-zinc-400">The page you are looking for does not exist.</p>
      <Link
        to="/"
        className="mt-8 inline-block rounded-lg border border-white/10 bg-white/5 px-4 py-2 text-sm text-zinc-200 transition hover:bg-white/10"
      >
        Back to home
      </Link>
    </section>
  )
}
