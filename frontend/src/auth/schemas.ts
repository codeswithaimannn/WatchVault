import { z } from 'zod'

/**
 * Form rules. They mirror the backend (RegisterRequest / LoginRequest) for quick feedback,
 * but the backend still checks everything: browser checks can be bypassed.
 */
export const PASSWORD_MIN_LENGTH = 12
export const PASSWORD_MAX_LENGTH = 72
const EMAIL_MAX_LENGTH = 254
const DISPLAY_NAME_MAX_LENGTH = 50

const passwordLengthMessage = `Password must be ${PASSWORD_MIN_LENGTH} to ${PASSWORD_MAX_LENGTH} characters.`

const email = z
  .string()
  .trim()
  .min(1, 'Email is required.')
  .max(EMAIL_MAX_LENGTH, 'Email is too long.')

export const loginSchema = z.object({
  email,
  password: z.string().min(1, 'Password is required.'),
})

export const registerSchema = z
  .object({
    displayName: z
      .string()
      .trim()
      .min(1, 'Name is required.')
      .max(DISPLAY_NAME_MAX_LENGTH, `Name must be at most ${DISPLAY_NAME_MAX_LENGTH} characters.`),
    email: email.pipe(z.email('Enter a valid email address.')),
    password: z
      .string()
      .min(PASSWORD_MIN_LENGTH, passwordLengthMessage)
      .max(PASSWORD_MAX_LENGTH, passwordLengthMessage),
    confirmPassword: z.string(),
  })
  .refine((values) => values.password === values.confirmPassword, {
    message: 'Passwords do not match.',
    path: ['confirmPassword'],
  })
