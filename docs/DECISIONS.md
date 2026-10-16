# Decisions log

Short record of key choices and why. Newest at the bottom.

---

## M0: External API rules (checked 2026-10-15)

Sources: TMDB API Terms of Use (dated 20 Oct 2023), TMDB developer FAQ, TMDB rate-limiting
docs, TMDB watch-providers reference, TVmaze API docs. Re-check these if the project changes shape.

### TMDB

**Caching**
- Terms §1.C: we may not cache any TMDB data "for longer than 6 months".
- Terms §1.D: if access ends, all TMDB content (including cache) must be deleted promptly.
- Decision: every cached TMDB record stores `fetched_at`. Anything older than 6 months is
  refreshed or deleted. Shorter refresh times will be set per data type in the caching milestone.
- TMDB's responses carry `Cache-Control: public, max-age=...` of roughly 1–8 hours. That's a
  useful hint for how stale the data can be in our short-term (Caffeine) cache.

**Attribution**
- Must show the TMDB logo (official, unmodified, less prominent than our own branding).
- Must show the notice: "This product uses the TMDB API but is not endorsed or certified by TMDB."
- Put both in an About/Credits section, link to https://www.themoviedb.org, and refer to it only as
  "TMDB" or "The Movie Database".
- Watch providers: the data comes from JustWatch and must be credited to JustWatch, or TMDB may
  revoke access. Decision: show "Streaming data from JustWatch" next to every provider list,
  and link to the `link` URL TMDB returns (we do not get direct deep links).

**Redistribution / storing TMDB data in a public repo**
- Neither the API terms nor the site terms mention git repositories or "redistribution" by name.
  The relevant wording:
  - API terms §1.A: the licence is "non-exclusive, non-transferable, non-sublicensable".
  - API terms §1.C, you must not: "Cache, for longer than 6 months, any information obtained
    through or from TMDB or the TMDB APIs"; "Make derivatives of the TMDB APIs or TMDB Content";
    "Sell, lease, or sublicense the TMDB APIs, access to the TMDB APIs, or TMDB Content, or derive
    revenues" (except as permitted in writing).
  - Site Terms of Use §3.A: "We grant You a non-exclusive license to use and copy the Materials
    and other users' Contributions solely for Your personal, non-commercial purposes". §3.B forbids
    you to "copy, download, reproduce, duplicate, archive, scrape, distribute, upload, publish ...
    the Materials" except as that personal, non-commercial licence allows.
- Decision: TMDB sample responses stay local only (gitignored). A permanent public copy would go
  past the 6-month cache limit and amounts to publishing TMDB Content to anyone. TVmaze samples
  (CC BY-SA 4.0) are committed with attribution.

**Commercial use**
- Free for non-commercial use. "Commercial" = primary purpose is revenue for the owner. Using TMDB
  data to train AI/ML is also listed as commercial use. Our scoring is rule-based, so that is fine.

**Rate limits**
- TMDB's docs: the old 40 requests / 10 seconds limit was **removed in December 2019**. The
  current upper limit is "somewhere in the 40 requests per second range" and may change. Over the
  limit returns HTTP `429`.
- Our sample calls returned **no rate-limit headers** (no remaining/reset counts).
- Decision: still throttle ourselves conservatively (target well under 40/s), and treat `429` as
  the real signal: back off and retry with Resilience4j, honouring `Retry-After` if present.

### TVmaze

- Licence: CC BY-SA 4.0. Credit TVmaze as the source (with link) wherever its data is shown.
- Rate limit: "at least 20 calls every 10 seconds per IP"; over it returns `429`. Back off and retry.
- TVmaze caches its own responses for 60 minutes (`Cache-Control: max-age=3600`), so asking more
  often than hourly gains nothing.
- `/lookup/shows?imdb=` replies with a **301 redirect** to the show. Our HTTP client must follow it.
  An unknown ID returns `404`.

### Data facts learned from the samples (affect later milestones)

1. **Linking TMDB to TVmaze**: TMDB `/tv/{id}/external_ids` gives `imdb_id` and `tvdb_id`;
   TVmaze `externals` holds the same (`imdb`, `thetvdb`). Plan: link shows by IMDb ID, with TVDB as
   a fallback.
2. **Runtimes**: TMDB TV `episode_run_time` was an **empty array** for Severance. Use per-episode
   `runtime` from season data. TMDB and TVmaze runtimes **disagree** (S1E1–3: TMDB 59/57/60, TVmaze
   57/53/56). Decision: one source (TMDB) is the main one for estimated viewing time, and it's
   always labelled as an estimate.
3. **Streaming release times are uncertain**: for Severance, TVmaze has `airtime: ""` and an
   `airstamp` of 12:00 UTC, which is a placeholder, not a real release time. Decision: when
   `airtime` is empty, show only the date ("Released on 18 Feb"), never a time.
4. **Two TVmaze schedules**: `/schedule?country=GB` covers broadcast TV only. Streaming/web releases
   (iPlayer, ITVX, Netflix…) are in `/schedule/web`. The calendar needs both.
5. **Genre IDs differ between movies and TV** (e.g. movie "Science Fiction" = 878, TV
   "Sci-Fi & Fantasy" = 10765). The mood mapping must store genres per media type.
6. **No "Anime" genre in TMDB.** Decision: anime = Animation genre (16) plus original language
   `ja` (Japanese).
7. **Watch providers have no region filter**: the endpoint returns ~90–110 countries; we keep `GB`.
   Lists can include duplicates of a service via a different route (e.g. "Apple TV" and
   "Apple TV Amazon Channel").
8. **Release dates**: movie `release_date` is the single main date (Dune: Part Two = 2024-02-27,
   the premiere), not the UK cinema date, even with `region=GB`. UK dates need
   `/movie/{id}/release_dates`. Until then, label it "Release date" rather than "UK release".
9. Multi search mixes `movie`, `tv` and `person` results; filter on `media_type`.

---

## M1: Users schema and test database (2026-10-15)

- **Preferred languages and genres live in child tables**, not JSON columns
  (`user_preferred_languages`, `user_preferred_genres`). The database enforces no duplicates and
  valid media types, and JPA maps them with a plain `@ElementCollection`. Genres are keyed by
  `(media_type, genre_id)` because TMDB movie and TV genre IDs differ. There is no FK to a genres
  table, because genre IDs are TMDB data and are validated in the app.
- **Times are `DATETIME(6)` in UTC.** `TIMESTAMP` ends in 2038 and converts time zones silently.
  The JDBC URL forces the session to UTC so `CURRENT_TIMESTAMP` defaults agree with Hibernate.
- **Email** is `VARCHAR(254)` with a unique constraint, which is the real guard against duplicates.
  The case-insensitive collation is a backstop, and the service also lowercases emails.
  **Password hash** is `VARCHAR(255)`, which leaves room for an algorithm change.
- **Tests use `watchvault_test` only**, via `src/test/resources/config/application.yaml`. It is on
  the test classpath only and overrides just the URL, so every test (Maven or IDE) is isolated
  without needing a profile annotation. A test asserts `DATABASE() = 'watchvault_test'`.

## M1: Authentication (2026-10-16)

- **JWT in an httpOnly cookie**, signed with HS256 using `WATCHVAULT_JWT_SECRET` (at least 32 bytes).
  The app both issues and checks tokens, so one shared secret is enough. Spring Security's
  resource server checks the signature, expiry and issuer on every request. The claims are the
  user id (`sub`) and `roles`, never the email.
- **8-hour lifetime and no refresh tokens.** JWTs can't be revoked before they expire, so logout
  only deletes the cookie, and a short lifetime limits the damage from a stolen token. Refresh
  tokens would add a lot of complexity for little gain here.
- **Cookie settings:** `HttpOnly` stops page JavaScript reading the token (XSS), `Secure` sends it
  over HTTPS only (browsers also allow it on localhost), and `SameSite=Lax` keeps it off
  cross-site form posts.
- **CSRF stays on** (`csrf.spa()`), because the browser sends the cookie automatically. It works
  as a double-submit check: the frontend calls `GET /api/auth/csrf` once, then copies the
  `XSRF-TOKEN` cookie into an `X-XSRF-TOKEN` header. Tests send the cookie and header themselves
  (`TestCsrf`) instead of using Spring's `csrf()` helper, which changes the shared test context.
- **The token is not read on `/api/auth/**`**, so an expired cookie can't block logging in again.
- **Passwords:** 12 to 72 characters, with length preferred over complexity rules (NCSC/NIST).
  BCrypt reads at most 72 bytes, so longer input is rejected rather than silently cut short.
  Hashes are stored as `{bcrypt}...` so the algorithm can change later.
- **Login gives the same answer for an unknown email and a wrong password**, and both take the
  same time (a dummy hash is checked when the email is unknown). Registration does reveal that an
  email is taken (409), which is an accepted trade-off for usable sign-up.
- **Rate limit:** 5 failed logins per IP within 15 minutes, then 429, even with the right password.
  It counts per IP rather than per email, so nobody can lock someone else out. It lives in memory
  (Caffeine), which is fine for one server. Behind a proxy it will need `X-Forwarded-For` handling
  (deploy milestone).
- **Ownership:** every `/me` endpoint takes the user id from the verified token, never from the URL
  or body. Tests prove that user A's changes don't affect user B.
- **Errors** use RFC 9457 problem details. Validation errors list each field, and unexpected errors
  return a generic 500 with the details only in the server log.

## M1: Frontend foundation (2026-10-16)

- **The Vite dev proxy forwards `/api` to `localhost:8080`.** The browser only ever talks to one
  origin, so the httpOnly cookie and CSRF token work without CORS. Production will serve both
  from one origin too (deploy milestone).
- **"Who am I" comes from `GET /api/users/me`,** cached by TanStack Query. JavaScript can't read
  the httpOnly cookie, so the server is the only source of truth. A 401 or 404 means "logged out".
- **One fetch wrapper (`api/client.ts`)** adds the CSRF header and turns problem-details errors
  into a typed `ApiError`. Only safe server messages are shown to the user.
- **Route guards:** `RequireAuth` sends visitors to `/login`, and `GuestOnly` is the single place
  that redirects after login. Redirects go back to the page that asked for login, but only to
  same-site paths (so links can't redirect to another website).
- **Forms use React Hook Form + Zod,** with rules mirroring the backend for quick feedback. The
  backend stays the authority. Server field errors are shown under the matching field.
- **Style (revised at the owner's request):** a dark, restrained theme with an ink background,
  slate greys, white text and one sky-to-teal gradient used sparingly (buttons, key words). There is no
  pink, purple or amber. The login/register backdrop is a drifting wall of poster shapes built from
  CSS gradients (not real posters, so there are no copyright or TMDB credit issues), and it respects
  reduce motion.
- **Display name:** collected at registration (V2 migration; existing accounts start with the part
  of their email before the @). It's user-written text, so React's escaping makes it safe to show.
- **Dependency versions:** React 19, React Router 8, TanStack Query 5, Zod 4, Tailwind 4, Vite 8,
  Vitest 5.

## M2: External API adapters (2026-10-16)

- **Retries use Spring Framework 7's built-in `RetryTemplate`, not Resilience4j** (the owner's
  choice). It needs no new dependency and carries no Spring Boot 4 compatibility risk. It retries
  429, 5xx, timeouts and connection failures (2 retries, 300ms then 600ms). It doesn't retry 404 or
  other 4xx, because asking again gives the same answer. Instead of a circuit breaker, we serve the
  saved copy from the database when an API stays down.
- **`Retry-After` is not read.** Spring's retry uses a fixed backoff schedule, and TMDB didn't send
  the header in the M0 samples. Waits are kept short because a user is waiting on the request.
- **HTTP client:** `RestClient` on Java's built-in `HttpClient`, with a 3s connect timeout, a 5s
  read timeout, and redirects followed (TVmaze lookups answer with a 301).
- **Errors:** a missing item becomes a 404 ("We couldn't find that title."). Anything else becomes
  a 503 ("TMDB is not responding right now…"). The details go to the server log only, and never
  include the token.
- **Tests use WireMock 3.13.2** (`wiremock-standalone`, test scope only; 4.x is still beta) with
  hand-written JSON. Real TMDB responses can't be committed (TMDB terms).
- **Two cache layers.** Caffeine (in memory) holds data that's cheap to fetch again: search 1h,
  genres 24h, providers 6h, seasons 6h, all within TMDB's Cache-Control hints. The database
  (`title_cache`, V3) holds title details, refreshed after 7 days. If TMDB is down, an older copy
  is served with its real `fetchedAt`. A copy older than **180 days** is never served, and a
  nightly job deletes such copies (TMDB terms: under 6 months).
- **`title_cache` stores the whole `TitleDetails` as a JSON column.** Nothing queries inside it
  yet, and fields can be promoted to real columns by a migration when a feature needs them.
- **User data never has a foreign key to `title_cache`.** Library rows will store only
  `media_type + tmdb_id`, because cache rows have to be deleted on a schedule.
- **One TMDB call per title** via `append_to_response` (details + release dates, or details +
  external ids). Watch providers stay a separate call because they change more often.
- **UK release date** = the earliest GB cinema release (type 2 or 3). It's null when there isn't
  one, and we never use another country's date. Checked against the live API: for Dune: Part Two
  it gives 1 March 2024 (TMDB's main date is the 27 Feb premiere).

## M4: Library (2026-10-16)

- **`library_entries` (V4) stores only user data plus `media_type + tmdb_id`.** Titles and posters
  come from `title_cache`, so no TMDB data is ever kept in user tables (TMDB's 6-month rule). If a
  title isn't cached and TMDB is down, the entry still shows with "Details unavailable right now".
- **Statuses:** Plan to watch, Watching, Completed, On hold, Dropped. **Ratings are whole numbers
  1-10** (the owner's choice; it matches TMDB's scale and is easy to average). A CHECK constraint
  enforces both in the database too.
- **`completed_on`** is set on the UK calendar day the entry becomes Completed, kept while it stays
  Completed, and cleared if it moves to another status. Statistics will rely on it.
- **Entries are addressed by title (`/api/library/movie/123`), never by a database id**, and the
  owner always comes from the token. There's nothing in a URL that could point at someone else's
  data. Tests check that user A can't see or change user B's library.
- **The list endpoint returns the whole library**, and the browser filters and counts it (a
  personal library is small). Title info is read from the cache in one query. At most 20 missing
  titles are fetched per request, and fetching stops at the first sign that TMDB is down.
- **No database transaction stays open while calling TMDB.** `LibraryService` isn't
  `@Transactional`, and each repository call commits on its own.
- **Validation errors have the same shape everywhere.** `HandlerMethodValidationException` (raised
  when Spring validates path params and body together) now also returns the `errors` map.
