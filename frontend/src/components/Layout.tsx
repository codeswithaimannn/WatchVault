import { Outlet, useNavigate } from 'react-router'

import { errorMessage } from '../api/client'
import { useCurrentUser, useLogout } from '../auth/authHooks'
import { Logo } from './Logo'

/** Frame for the logged-in app: glass header that stays at the top, then the page. */
export function Layout() {
  const { data: user } = useCurrentUser()

  return (
    <div className="flex min-h-screen flex-col">
      <header className="sticky top-0 z-20 border-b border-white/5 bg-ink/70 backdrop-blur-xl">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3.5">
          <Logo />
          {user && <LogoutButton />}
        </div>
      </header>
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-12">
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
        <span role="alert" className="text-sm text-rose-300">
          {errorMessage(logout.error)}
        </span>
      )}
      <button
        type="button"
        onClick={handleLogout}
        disabled={logout.isPending}
        className="rounded-lg border border-white/10 bg-white/5 px-3.5 py-1.5 text-sm text-zinc-300 transition hover:border-white/20 hover:bg-white/10 hover:text-white disabled:opacity-60"
      >
        {logout.isPending ? 'Logging out…' : 'Log out'}
      </button>
    </div>
  )
}
