import { useCurrentUser } from '../auth/authHooks'

/** Placeholder home. Library, "continue watching" and recommendations arrive in later milestones. */
export function HomePage() {
  const { data: user } = useCurrentUser()

  return (
    <section className="flex flex-col gap-6">
      <div>
        <p className="text-sm uppercase tracking-widest text-amber-400">Your vault</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight">Welcome, {user?.email}</h1>
      </div>
      <div className="rounded-xl border border-dashed border-zinc-800 p-10 text-center text-zinc-400">
        <p className="text-lg text-zinc-300">Your library is empty for now.</p>
        <p className="mt-2 text-sm">Search and adding titles are coming soon.</p>
      </div>
    </section>
  )
}
