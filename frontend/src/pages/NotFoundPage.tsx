import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <section className="py-24 text-center">
      <p className="text-6xl font-bold text-amber-400">404</p>
      <h1 className="mt-4 text-2xl font-semibold">Page not found</h1>
      <Link to="/" className="mt-6 inline-block text-amber-400 hover:text-amber-300">
        Back to home
      </Link>
    </section>
  )
}
