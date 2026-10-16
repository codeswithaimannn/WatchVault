/**
 * Where to go after logging in: back to the page that sent us to /login, or home.
 * Only same-site paths are allowed ("/library", not "//evil.example"), so a crafted
 * link cannot use our login page to redirect someone to another website.
 */
export function redirectTargetAfterLogin(locationState: unknown): string {
  if (typeof locationState === 'object' && locationState !== null && 'from' in locationState) {
    const from = locationState.from
    if (typeof from === 'string' && from.startsWith('/') && !from.startsWith('//')) {
      return from
    }
  }
  return '/'
}
