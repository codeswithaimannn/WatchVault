import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { authApi, type Registration, type User } from '../api/auth'
import { ApiError } from '../api/client'

/** TanStack Query cache key for "who is logged in". */
export const CURRENT_USER_KEY = ['currentUser'] as const

const FIVE_MINUTES_MS = 5 * 60 * 1000

/**
 * The logged-in user, or null when nobody is. JavaScript cannot read the httpOnly cookie,
 * so the only way to know is to ask the backend.
 */
export function useCurrentUser() {
  return useQuery({
    queryKey: CURRENT_USER_KEY,
    queryFn: fetchCurrentUserOrNull,
    staleTime: FIVE_MINUTES_MS,
    retry: false,
  })
}

async function fetchCurrentUserOrNull(): Promise<User | null> {
  try {
    return await authApi.currentUser()
  } catch (error) {
    // 401: not logged in (or token expired). 404: the account was deleted.
    if (error instanceof ApiError && (error.status === 401 || error.status === 404)) {
      return null
    }
    throw error
  }
}

export function useLogin() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: authApi.login,
    onSuccess: (user) => queryClient.setQueryData(CURRENT_USER_KEY, user),
  })
}

/** Registers, then logs straight in, so a new user lands on their home page. */
export function useRegister() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (registration: Registration) => {
      await authApi.register(registration)
      return authApi.login({ email: registration.email, password: registration.password })
    },
    onSuccess: (user) => queryClient.setQueryData(CURRENT_USER_KEY, user),
  })
}

export function useLogout() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: authApi.logout,
    onSuccess: () => {
      queryClient.clear() // forget every cached response that belonged to this user
      queryClient.setQueryData(CURRENT_USER_KEY, null)
    },
  })
}
