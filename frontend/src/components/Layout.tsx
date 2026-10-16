import { Link, Outlet, useNavigate } from 'react-router'

import { errorMessage } from '../api/client'
import { useCurrentUser, useLogout } from '../auth/authHooks'

/** Header and page frame shared by every page. */
export function Layout() {
  const { data: user } = useCurrentUser()

  return (
    <div className="flex min-h-screen flex-col">
      <header className="border-b border-zinc-800/80 bg-zinc-950/80 backdrop-blur">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-4">
          <Link to="/" className="text-xl font-bold tracking-tight">
            Watch<span className="text-amber-400">Vault</span>
          </Link>
          {user && <LogoutButton />}
        </div>
      </header>
      <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-12">
        <Outlet />
      </main>
    </div>
  )
}

function LogoutButton() {
  const logout = useLogout()
  const navigate = useNavigate()

  const handleLogout = () =>
    logout.mutate(undefined, { onSuccess: () => navigate('/login', { replace: true }) })

  return (
    <div className="flex items-center gap-3">
      {logout.isError && (
        <span role="alert" className="text-sm text-red-400">
          {errorMessage(logout.error)}
        </span>
      )}
      <button
        type="button"
        onClick={handleLogout}
        disabled={logout.isPending}
        className="rounded-md border border-zinc-700 px-3 py-1.5 text-sm text-zinc-300 hover:border-zinc-500 hover:text-zinc-100 disabled:opacity-60"
      >
        {logout.isPending ? 'Logging out…' : 'Log out'}
      </button>
    </div>
  )
}
