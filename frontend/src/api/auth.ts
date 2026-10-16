import { apiRequest } from './client'

/** Mirrors the backend's UserResponse DTO. */
export interface User {
  id: number
  email: string
  role: 'USER' | 'ADMIN'
}

export interface Credentials {
  email: string
  password: string
}

export const authApi = {
  register: (credentials: Credentials) =>
    apiRequest<User>('/api/auth/register', { method: 'POST', body: credentials }),

  /** On success the backend sets the httpOnly login cookie; we never see the token itself. */
  login: (credentials: Credentials) =>
    apiRequest<User>('/api/auth/login', { method: 'POST', body: credentials }),

  logout: () => apiRequest<void>('/api/auth/logout', { method: 'POST' }),

  currentUser: () => apiRequest<User>('/api/users/me'),
}
