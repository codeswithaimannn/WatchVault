import type { ReactNode } from 'react'

interface AuthCardProps {
  title: string
  children: ReactNode
  footer: ReactNode
}

/** The centred panel used by the login and register pages. */
export function AuthCard({ title, children, footer }: AuthCardProps) {
  return (
    <div className="mx-auto w-full max-w-sm">
      <div className="rounded-xl border border-zinc-800 bg-zinc-900/60 p-8 shadow-2xl shadow-black/40">
        <h1 className="mb-6 text-2xl font-semibold tracking-tight">{title}</h1>
        {children}
      </div>
      <p className="mt-6 text-center text-sm text-zinc-400">{footer}</p>
    </div>
  )
}

/** A form-level error, e.g. "Invalid email or password." */
export function FormAlert({ children }: { children: ReactNode }) {
  return (
    <p role="alert" className="rounded-md border border-red-900 bg-red-950/50 px-3 py-2 text-sm text-red-300">
      {children}
    </p>
  )
}

export function SubmitButton({ children, busy }: { children: ReactNode; busy: boolean }) {
  return (
    <button
      type="submit"
      disabled={busy}
      className="w-full rounded-md bg-amber-400 px-4 py-2.5 font-semibold text-zinc-950 hover:bg-amber-300 focus:outline-none focus:ring-2 focus:ring-amber-400 focus:ring-offset-2 focus:ring-offset-zinc-950 disabled:cursor-not-allowed disabled:opacity-60"
    >
      {children}
    </button>
  )
}
