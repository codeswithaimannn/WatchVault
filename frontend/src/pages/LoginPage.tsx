import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router'

import { errorMessage } from '../api/client'
import { useLogin } from '../auth/authHooks'
import { loginSchema } from '../auth/schemas'
import { AuthCard, FormAlert, SubmitButton } from '../components/AuthCard'
import { TextField } from '../components/TextField'

/** After a successful login, GuestOnly (routeGuards.tsx) sees the user and redirects. */
export function LoginPage() {
  const login = useLogin()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({ resolver: zodResolver(loginSchema) })

  const onSubmit = handleSubmit((credentials) => login.mutate(credentials))

  return (
    <AuthCard
      title="Sign in"
      subtitle="Welcome back. Your watchlist missed you."
      footer={
        <>
          New to WatchVault?{' '}
          <Link to="/register" className="text-brand-gradient font-semibold hover:brightness-125">
            Create an account
          </Link>
        </>
      }
    >
      {/* noValidate: show our own messages instead of the browser's built-in pop-ups */}
      <form onSubmit={onSubmit} noValidate className="flex flex-col gap-5">
        {login.isError && <FormAlert>{errorMessage(login.error)}</FormAlert>}
        <TextField
          label="Email"
          type="email"
          autoComplete="email"
          error={errors.email?.message}
          {...register('email')}
        />
        <TextField
          label="Password"
          type="password"
          autoComplete="current-password"
          error={errors.password?.message}
          {...register('password')}
        />
        <SubmitButton busy={login.isPending}>{login.isPending ? 'Signing in…' : 'Sign in'}</SubmitButton>
      </form>
    </AuthCard>
  )
}
