# WatchVault: Personal Entertainment Hub

A full-stack web app for tracking movies, TV series and anime: personal library, episode-level
progress, discovery, UK streaming availability, explainable recommendations (mood + time available),
statistics, upcoming-episode calendar, notifications and a franchise explorer.

This is a portfolio project. The owner is a junior developer who must be able to explain every part
in a job interview. Explain decisions briefly, and never generate large amounts of code the owner has
not been walked through.

## Working agreement
- Build ONE milestone at a time, in the order of docs/PLAN.md. Do not start the next milestone until
  the current one runs, is tested, and the owner confirms.
- Before writing code for a milestone: state the plan in a few bullets and wait for approval.
- After each step: tell the owner how to run it and how to check it works.
- Ask before adding any new dependency or making a decision that is hard to reverse.
- Commit small and often, with clear messages. Never commit secrets.

## Stack
- Backend: Java (JDK 25 LTS), Spring Boot, Maven, Spring Data JPA, Flyway, Spring Security,
  Resilience4j, Spring Cache (Caffeine), Spring Mail, springdoc OpenAPI
- Database: MySQL
- Frontend: React + TypeScript + Vite, Tailwind CSS, TanStack Query, React Router,
  React Hook Form + Zod, Recharts
- Tests: JUnit 5, Mockito, WireMock (backend); Vitest + React Testing Library (frontend)
- No Docker for now.

## Layout
- backend/   Spring Boot app (controller -> service -> repository; adapters for external APIs)
- frontend/  React app
- docs/      PLAN.md (milestones), DECISIONS.md (short log of key choices and why)

## Architecture rules
- The browser NEVER calls TMDB or TVmaze directly. All external calls go through backend adapters.
- External metadata is cached with a fetched_at timestamp. User data (library, ratings, progress)
  lives in our own database and must keep working if an external API is down.
- Keep API-sourced data, user-recorded data and curated data (franchise orders, mood mapping)
  clearly separated. Recorded viewing time and estimated viewing time are different things.
- Recommendations use a transparent weighted score with hard filters first. Every recommendation
  carries a plain-English reason. No ML.
- Never present unavailable or uncertain data as fact (release dates, providers, episode schedules).

## External APIs
- TMDB: primary source. Server-side credentials only. Respect rate limits (commonly documented
  as about 40 requests per 10 seconds; verify in M0). Show the required TMDB logo and
  "not endorsed or certified by TMDB" notice in an About/Credits section. Credit JustWatch next
  to streaming availability. Check TMDB's terms on how long data may be cached.
- TVmaze: schedules and upcoming episodes. Keyless, at least 20 calls per 10 seconds per IP.
  Data is CC BY-SA 4.0, so attribution to TVmaze is required.
- Project is non-commercial. Do not add monetisation or ads.

## Security rules
- Keys and passwords only in environment variables or an untracked config file; keep a
  committed example file with placeholder values.
- BCrypt for passwords; JWT in an httpOnly cookie; rate-limit login.
- Every library/progress/list endpoint must check the record belongs to the logged-in user.
- Validate all input. Escape user-written text (reviews, notes). Return safe error messages.
- Provide account deletion and data export (UK GDPR).

## Testing rules
- Unit tests for scoring, time-fitting, next-episode logic and statistics.
- Adapter tests with WireMock and saved sample responses, including failure cases.
- Authorisation tests: user A cannot read or change user B's data.
- One end-to-end path: search, add, mark episode watched, see statistics update.

## Code style
- Small, readable methods. Meaningful names. No unexplained magic numbers.
- Backend: constructor injection, DTOs for API responses (never expose entities directly).
- Frontend: TypeScript strict mode, function components, handle loading/empty/error states.
