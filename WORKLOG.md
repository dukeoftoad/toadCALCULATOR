# WORKLOG

Append-only handoff log. One entry per phase, UTC timestamped.
Each entry: phase ID, summary, files created/modified, tests run (pass/fail),
decisions with rationale, open questions, next task ID.

---

## Phase −1 — repository and CI — 2025-06-10T00:00:00Z (entry timestamped at commit time)

**Summary.** Created private GitHub repository `dukeoftoad/toadCALCULATOR` via the GitHub REST API
(`gh` CLI unavailable in the environment; token supplied through `GH_TOKEN`, never hardcoded or
logged). Initialized with GPL-3.0-or-later LICENSE (canonical SPDX text), Android `.gitignore`,
append-only `WORKLOG.md`, and `.github/workflows/build.yml`. The workflow runs on push to `main`,
on pull requests, and on manual dispatch; it sets up JDK 21 Temurin, uses `gradle/actions/setup-gradle`
for dependency caching, runs unit tests for all modules, builds debug and release APKs, uploads both
as artifacts with 30-day retention, fails if any test fails, and fails if the release APK exceeds
4 MiB (4,194,304 bytes). A second job (`permissions-audit`) assembles the release APK and prints the
merged manifest permission list via `aapt2 dump permissions` so accidental permissions are visible in
the log. No secrets are required to build; nothing is published anywhere.

**Files.** `.github/workflows/build.yml`, `.gitignore`, `LICENSE`, `WORKLOG.md`.

**Tests.** None (infrastructure phase). Placeholder removed on first real entry.

**Decisions.**
- `gh` CLI absent → REST API via `curl` with `Authorization: Bearer $GH_TOKEN` (per spec fallback).
- Git pushes authenticate through a throwaway `credential.helper` that reads `GH_TOKEN` from the
  process environment, so the token never appears in argv, remote URLs, or any tracked file.
- LICENSE text fetched from `spdx/license-list-data` (gnu.org unreachable from this sandbox); the
  text is the canonical GPL-3.0 body; "or later" is asserted by SPDX headers on all sources.
- Size limit encoded as 4,194,304 bytes (4 MiB), matching the spec.
- Permissions-audit job is informational (prints list) per spec; a warning is printed if non-empty.

**Open questions.** None.

**Next task ID.** 0 (project scaffolding).
