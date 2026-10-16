import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router'

import { ApiError, errorMessage } from '../api/client'
import { useRegister } from '../auth/authHooks'
import { PASSWORD_MIN_LENGTH, registerSchema } from '../auth/schemas'
import { AuthCard, FormAlert, SubmitButton } from '../components/AuthCard'
import { TextField } from '../components/TextField'

const SERVER_FIELDS = ['email', 'password'] as const

export function RegisterPage() {
  const registration = useRegister()
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm({ resolver: zodResolver(registerSchema) })

  const onSubmit = handleSubmit(({ email, password }) =>
    registration.mutate(
      { email, password },
      {
        // On success GuestOnly (routeGuards.tsx) sees the new user and redirects home.
        // If the server rejects a field, show its message under that field.
        onError: (error) => {
          if (error instanceof ApiError) {
            for (const field of SERVER_FIELDS) {
              const message = error.fieldErrors[field]
              if (message) setError(field, { message })
            }
          }
        },
      },
    ),
  )

  const hasFieldErrorsFromServer =
    registration.error instanceof ApiError && Object.keys(registration.error.fieldErrors).length > 0

  return (
    <AuthCard
      title="Create your account"
      footer={
        <>
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-amber-400 hover:text-amber-300">
            Sign in
          </Link>
        </>
      }
    >
      <form onSubmit={onSubmit} noValidate className="flex flex-col gap-5">
        {registration.isError && !hasFieldErrorsFromServer && (
          <FormAlert>{errorMessage(registration.error)}</FormAlert>
        )}
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
          autoComplete="new-password"
          placeholder={`At least ${PASSWORD_MIN_LENGTH} characters`}
          error={errors.password?.message}
          {...register('password')}
        />
        <TextField
          label="Confirm password"
          type="password"
          autoComplete="new-password"
          error={errors.confirmPassword?.message}
          {...register('confirmPassword')}
        />
        <SubmitButton busy={registration.isPending}>
          {registration.isPending ? 'Creating account…' : 'Create account'}
        </SubmitButton>
      </form>
    </AuthCard>
  )
}
