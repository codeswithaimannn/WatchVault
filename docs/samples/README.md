# API samples (Milestone 0)

Real responses captured on 2026-10-15, used as a reference and as a basis for test fixtures.

- **Only the TVmaze samples are committed.** The TMDB samples (`tmdb_*.json`) and `_call_log.json`
  are kept locally and are gitignored, because TMDB's terms forbid caching their data for more
  than 6 months and forbid sublicensing TMDB Content (see docs/DECISIONS.md).
- Each file is `{ "_request": url, "_fetched_on": date, "response": ... }`.
- Lists are **trimmed** to the first 3 items; a `{"_trimmed": ...}` entry says how many were cut.
- Example show: Severance (TMDB 95396 / IMDb tt11280740 / TVmaze 44933).

Committed files:
- `tvmaze_lookup_by_imdb.json`: `/lookup/shows?imdb=` (the API answers with a 301 redirect to the show)
- `tvmaze_show_episodes.json`: `/shows/{id}/episodes`
- `tvmaze_schedule_GB_today.json`: `/schedule?country=GB` (broadcast TV)
- `tvmaze_schedule_web_GB_today.json`: `/schedule/web?country=GB` (streaming/web)

Data source: [TVmaze](https://www.tvmaze.com), licensed under
[CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/).
