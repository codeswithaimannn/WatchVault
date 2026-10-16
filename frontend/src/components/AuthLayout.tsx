import { Outlet } from 'react-router'

import { Logo } from './Logo'
import { PosterWall } from './PosterWall'

/** Full-screen cinematic frame for the login and register pages. */
export function AuthLayout() {
  return (
    <div className="relative min-h-screen overflow-hidden">
      <PosterWall />

      <div className="relative grid min-h-screen lg:grid-cols-2">
        {/* Left: brand and tagline (wide screens only). */}
        <section className="hidden flex-col justify-between p-12 lg:flex">
          <Logo />
          <div className="max-w-md">
            <p className="text-4xl leading-tight font-semibold tracking-tight">
              Every film. Every episode. <span className="text-brand-gradient">One vault.</span>
            </p>
            <p className="mt-4 text-zinc-400">
              Track movies, series and anime, see where they are streaming in the UK, and pick up exactly
              where you left off.
            </p>
          </div>
          <p className="text-xs text-zinc-500">WatchVault · a personal entertainment hub</p>
        </section>

        {/* Right: the form. */}
        <section className="flex flex-col items-center justify-center gap-10 px-4 py-12">
          <div className="lg:hidden">
            <Logo />
          </div>
          <Outlet />
        </section>
      </div>
    </div>
  )
}
