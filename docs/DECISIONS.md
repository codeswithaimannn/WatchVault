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
