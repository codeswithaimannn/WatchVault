import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { fakeBackend, giveCsrfCookie, json, noContent, renderApp, unauthorized } from '../test/testUtils'

const ALICE = { id: 1, email: 'alice@example.com', role: 'USER' }
const PASSWORD = 'correct horse battery'

describe('login', () => {
  it('shows validation messages and does not call the server for an empty form', async () => {
    const requests = fakeBackend({ 'GET /api/users/me': unauthorized })
    renderApp('/login')

    await userEvent.click(await screen.findByRole('button', { name: 'Sign in' }))

    expect(await screen.findByText('Email is required.')).toBeInTheDocument()
    expect(screen.getByText('Password is required.')).toBeInTheDocument()
    expect(requests.some((r) => r.url === '/api/auth/login')).toBe(false)
  })

  it("shows the server's message for wrong credentials", async () => {
    giveCsrfCookie()
    fakeBackend({
      'GET /api/users/me': unauthorized,
      'POST /api/auth/login': () => json(401, { detail: 'Invalid email or password.' }),
    })
    renderApp('/login')

    await fillAndSubmitLogin('alice@example.com', 'wrong password')

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password.')
  })

  it('logs in and lands on the home page', async () => {
    giveCsrfCookie()
    fakeBackend({
      'GET /api/users/me': unauthorized,
      'POST /api/auth/login': () => json(200, ALICE),
    })
    renderApp('/login')

    await fillAndSubmitLogin(ALICE.email, PASSWORD)

    expect(await screen.findByRole('heading', { name: 'Welcome, alice@example.com' })).toBeInTheDocument()
  })
})

describe('protected pages', () => {
  it('sends a logged-out visitor to the login page', async () => {
    fakeBackend({ 'GET /api/users/me': unauthorized })

    renderApp('/')

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
  })

  it('shows a retry option when the server is down', async () => {
    fakeBackend({}) // every request fails like a network error

    renderApp('/')

    expect(await screen.findByText('Cannot reach the server.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument()
  })

  it('logs out and returns to the login page', async () => {
    giveCsrfCookie()
    fakeBackend({
      'GET /api/users/me': () => json(200, ALICE),
      'POST /api/auth/logout': noContent,
    })
    renderApp('/')

    await userEvent.click(await screen.findByRole('button', { name: 'Log out' }))

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
  })
})

describe('register', () => {
  it('checks that the passwords match before calling the server', async () => {
    const requests = fakeBackend({ 'GET /api/users/me': unauthorized })
    renderApp('/register')

    await userEvent.type(await screen.findByLabelText('Email'), 'new@example.com')
    await userEvent.type(screen.getByLabelText('Password'), PASSWORD)
    await userEvent.type(screen.getByLabelText('Confirm password'), 'something else entirely')
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByText('Passwords do not match.')).toBeInTheDocument()
    expect(requests.some((r) => r.url === '/api/auth/register')).toBe(false)
  })

  it('shows a server field error under the right field', async () => {
    giveCsrfCookie()
    fakeBackend({
      'GET /api/users/me': unauthorized,
      'POST /api/auth/register': () =>
        json(400, { detail: 'Some fields are invalid.', errors: { email: 'Email is too long.' } }),
    })
    renderApp('/register')

    await fillAndSubmitRegister('new@example.com', PASSWORD)

    expect(await screen.findByText('Email is too long.')).toBeInTheDocument()
  })

  it('creates the account, logs in and lands on the home page', async () => {
    giveCsrfCookie()
    const requests = fakeBackend({
      'GET /api/users/me': unauthorized,
      'POST /api/auth/register': () => json(201, ALICE),
      'POST /api/auth/login': () => json(200, ALICE),
    })
    renderApp('/register')

    await fillAndSubmitRegister(ALICE.email, PASSWORD)

    expect(await screen.findByRole('heading', { name: 'Welcome, alice@example.com' })).toBeInTheDocument()
    const registerRequest = requests.find((r) => r.url === '/api/auth/register')
    expect(registerRequest?.body).toEqual({ email: ALICE.email, password: PASSWORD }) // no confirmPassword
  })
})

async function fillAndSubmitLogin(email: string, password: string) {
  await userEvent.type(await screen.findByLabelText('Email'), email)
  await userEvent.type(screen.getByLabelText('Password'), password)
  await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
}

async function fillAndSubmitRegister(email: string, password: string) {
  await userEvent.type(await screen.findByLabelText('Email'), email)
  await userEvent.type(screen.getByLabelText('Password'), password)
  await userEvent.type(screen.getByLabelText('Confirm password'), password)
  await userEvent.click(screen.getByRole('button', { name: 'Create account' }))
}
