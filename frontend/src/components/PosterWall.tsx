/**
 * Decorative backdrop for the login/register pages: tilted columns of poster-shaped
 * gradient tiles drifting slowly, dimmed by dark fades and film grain.
 * Pure CSS: no real posters (copyrighted, and TMDB images need TMDB credit).
 */

// Deep, muted colours so the wall stays in the background. Full class names, so Tailwind finds them.
const POSTER_COLOURS = [
  'from-indigo-800 to-slate-950',
  'from-rose-900 to-zinc-950',
  'from-amber-700 to-stone-950',
  'from-violet-800 to-indigo-950',
  'from-teal-800 to-slate-950',
  'from-fuchsia-900 to-zinc-950',
  'from-sky-800 to-slate-950',
  'from-orange-800 to-neutral-950',
]

const COLUMN_COUNT = 7
const POSTERS_PER_COLUMN = 6

export function PosterWall() {
  return (
    <div aria-hidden="true" className="pointer-events-none absolute inset-0 overflow-hidden">
      <div className="absolute -inset-1/4 flex -rotate-12 gap-4 opacity-40">
        {Array.from({ length: COLUMN_COUNT }, (_, column) => (
          <PosterColumn key={column} column={column} />
        ))}
      </div>

      {/* Fades: darker towards the form side and the edges, so text stays readable. */}
      <div className="absolute inset-0 bg-linear-to-b from-ink/70 via-ink/50 to-ink" />
      <div className="absolute inset-0 bg-ink/55 lg:bg-transparent lg:bg-linear-to-r lg:from-transparent lg:via-ink/40 lg:to-ink/90" />
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,transparent_30%,var(--color-ink)_85%)]" />

      {/* Projector glow from the top-left, very faint. */}
      <div className="absolute -top-40 -left-40 size-[42rem] rounded-full bg-violet-600/15 blur-3xl" />
      <div className="absolute -right-32 bottom-0 size-[30rem] rounded-full bg-rose-500/10 blur-3xl" />

      <div className="film-grain absolute inset-0 opacity-[0.06] mix-blend-overlay" />
    </div>
  )
}

/**
 * One column. Its posters are rendered twice, so sliding up by half the height
 * lands on an identical picture: the loop has no visible jump.
 */
function PosterColumn({ column }: { column: number }) {
  const posters = Array.from({ length: POSTERS_PER_COLUMN }, (_, row) => (column * 3 + row * 5) % POSTER_COLOURS.length)
  const reverse = column % 2 === 1

  return (
    <div
      className={`flex w-40 shrink-0 flex-col gap-4 animate-drift motion-reduce:animate-none ${
        reverse ? '[animation-direction:reverse]' : ''
      }`}
    >
      {[...posters, ...posters].map((colour, index) => (
        <Poster key={index} colour={POSTER_COLOURS[colour]} />
      ))}
    </div>
  )
}

function Poster({ colour }: { colour: string }) {
  return (
    <div className={`relative aspect-2/3 overflow-hidden rounded-lg bg-linear-to-b ${colour} ring-1 ring-white/5`}>
      {/* Hint of a title block, like the bottom of a film poster. */}
      <div className="absolute inset-x-3 bottom-4 flex flex-col gap-1.5">
        <div className="h-2 w-3/4 rounded-full bg-white/20" />
        <div className="h-1.5 w-1/2 rounded-full bg-white/10" />
      </div>
    </div>
  )
}
