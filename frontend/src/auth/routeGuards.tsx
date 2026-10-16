import { Navigate, Outlet, useLocation } from 'react-router'

import { StatusMessage } from '../components/StatusMessage'
import { useCurrentUser } from './authHooks'
import { redirectTargetAfterLogin } from './redirect'

/** Pages inside this route need a login; anyone else is sent to /login and brought back after. */
export function RequireAuth() {
  const { data: user, isPending, isError, refetch } = useCurrentUser()
  const location = useLocation()

  if (isPending) {
    return <StatusMessage>Loading…</StatusMessage>
  }
  if (isError) {
    return <StatusMessage onRetry={() => void refetch()}>Cannot reach the server.</StatusMessage>
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return <Outlet />
}

/**
 * Login and register pages. As soon as there is a user (already logged in, or the login form
 * just succeeded and stored the user), leave: back to the page that sent us here, or home.
 * This is the single place that redirects after login.
 */
export function GuestOnly() {
  const { data: user, isPending } = useCurrentUser()
  const location = useLocation()

  if (isPending) {
    return <StatusMessage>Loading…</StatusMessage>
  }
  if (user) {
    return <Navigate to={redirectTargetAfterLogin(location.state)} replace />
  }
  return <Outlet />
}
