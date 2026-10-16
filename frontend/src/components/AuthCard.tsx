import type { ReactNode } from 'react'

interface AuthCardProps {
  title: string
  subtitle?: string
  children: ReactNode
  footer: ReactNode
}

/** Frosted-glass panel used by the login and register pages. */
export function AuthCard({ title, subtitle, children, footer }: AuthCardProps) {
  return (
    <div className="w-full max-w-sm">
      <div className="rounded-2xl border border-white/10 bg-slate-950/60 p-8 shadow-2xl shadow-black/60 backdrop-blur-xl">
        <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
        {subtitle && <p className="mt-1.5 text-sm text-slate-400">{subtitle}</p>}
        <div className="mt-7">{children}</div>
      </div>
      <p className="mt-6 text-center text-sm text-slate-400">{footer}</p>
    </div>
  )
}

/** A form-level error, e.g. "Invalid email or password." */
export function FormAlert({ children }: { children: ReactNode }) {
  return (
    <p role="alert" className="rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-2 text-sm text-red-200">
      {children}
    </p>
  )
}

export function SubmitButton({ children, busy }: { children: ReactNode; busy: boolean }) {
  return (
    <button
      type="submit"
      disabled={busy}
      className="bg-brand-gradient w-full rounded-lg px-4 py-2.5 font-semibold text-white shadow-lg shadow-sky-900/30 transition hover:brightness-110 focus:ring-2 focus:ring-sky-400/60 focus:ring-offset-2 focus:ring-offset-slate-950 focus:outline-none disabled:cursor-not-allowed disabled:opacity-60"
    >
      {children}
    </button>
  )
}
