import { Outlet, useNavigate, useSearchParams } from 'react-router'

import { errorMessage } from '../api/client'
import { useCurrentUser, useLogout } from '../auth/authHooks'
import { SearchBar } from '../titles/SearchBar'
import { Logo } from './Logo'

/** Frame for the logged-in app: glass header that stays at the top, then the page. */
export function Layout() {
  const { data: user } = useCurrentUser()
  const [searchParams] = useSearchParams()
  const currentQuery = searchParams.get('q') ?? ''

  return (
    <div className="flex min-h-screen flex-col">
      <header className="sticky top-0 z-20 border-b border-white/5 bg-ink/70 backdrop-blur-xl">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-6 gap-y-3 px-4 py-3.5">
          <Logo />
          {user && (
            <>
              {/* order: on phones the search bar drops to its own full-width row */}
              <div className="order-last flex w-full justify-center sm:order-none sm:w-auto sm:flex-1">
                <SearchBar key={currentQuery} initialQuery={currentQuery} />
              </div>
              <div className="ml-auto sm:ml-0">
                <LogoutButton />
              </div>
            </>
          )}
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
        <span role="alert" className="text-sm text-red-300">
          {errorMessage(logout.error)}
        </span>
      )}
      <button
        type="button"
        onClick={handleLogout}
        disabled={logout.isPending}
        className="rounded-lg border border-white/10 bg-white/5 px-3.5 py-1.5 text-sm text-slate-300 transition hover:border-white/20 hover:bg-white/10 hover:text-white disabled:opacity-60"
      >
        {logout.isPending ? 'Logging out…' : 'Log out'}
      </button>
    </div>
  )
}
