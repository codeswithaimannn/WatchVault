/** Display formatting, British English. Unknown values return null so the UI can say so honestly. */

const MINUTES_PER_HOUR = 60

// Dates arrive as "yyyy-MM-dd" (no time zone). Formatting in UTC stops them shifting a day.
const fullDate = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'long', year: 'numeric', timeZone: 'UTC' })
const languageNames = new Intl.DisplayNames(['en-GB'], { type: 'language' })

/** "2024-03-01" -> "1 March 2024". */
export function formatDate(isoDate: string | null): string | null {
  if (!isoDate) return null
  const date = new Date(isoDate)
  return Number.isNaN(date.getTime()) ? null : fullDate.format(date)
}

/** "2024-03-01" -> "2024". */
export function yearOf(isoDate: string | null): string | null {
  return isoDate && /^\d{4}/.test(isoDate) ? isoDate.slice(0, 4) : null
}

/** 167 -> "2 h 47 min"; 45 -> "45 min". */
export function formatRuntime(minutes: number | null): string | null {
  if (minutes === null || minutes <= 0) return null
  const hours = Math.floor(minutes / MINUTES_PER_HOUR)
  const rest = minutes % MINUTES_PER_HOUR
  if (hours === 0) return `${rest} min`
  return rest === 0 ? `${hours} h` : `${hours} h ${rest} min`
}

/** "ja" -> "Japanese". Falls back to the code itself if the browser doesn't know it. */
export function languageName(code: string | null): string | null {
  if (!code) return null
  try {
    return languageNames.of(code) ?? code
  } catch {
    return code
  }
}
