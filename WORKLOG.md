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

---

## Phase 0 — project scaffolding

**Summary.** Multi-module Gradle (Kotlin DSL) project: `:app` (com.toadcalculator), `:core-math`
(pure Kotlin, com.toadcalculator.math), `:core-data` (com.toadcalculator.data). Kotlin 2.1.21,
AGP 8.10.1, Gradle 8.13 (wrapper committed), Java toolchain 21, compileSdk/targetSdk 36, minSdk 26.
Compose + Material 3 theme with system/light/dark and dynamic color OFF by default; toad-green
brand palette. Debug and release build types; R8 + resource shrinking in release; manifest has zero
permissions. Adaptive launcher icon (vector foreground, monochrome layer) and a `Theme.SplashScreen`
splash matching the theme. `:core-math` contains an AngleMode/MathResult placeholder plus one passing
Truth test and a Gradle `verifyNoAndroidImports` guard hooked into `check`. `:core-data` already
carries the full Phase 3/5 backends (Room entity/DAO/database, HistoryRepository interface + Room
impl, SettingsRepository interface + DataStore impl) and Robolectric DAO tests, because they are
small and let Phase 3 wire UI directly. Manual DI via `AppGraph` (no Hilt — decision below).
README with build commands, architecture diagram, GPL notice, CI badge.

**Files.** settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml,
gradle wrapper (jar/scripts), core-math/* (build + 2 sources + 1 test), core-data/* (build,
manifest, consumer rules, 6 sources, 1 test), app/* (build, proguard, manifest, res: strings/colors/
themes/night-colors/adaptive icons/splash, MainActivity, AppGraph, ui/AppRoot, ui/theme/{Color,
Theme,Type}), README.md, WORKLOG.md, .github/workflows/build.yml (from Phase −1).

**Tests.** `:core-math:test`: 1 pass / 0 fail locally (JDK 21, Gradle 8.13).
`:core-math:verifyNoAndroidImports`: OK. Android-module tests run in CI (no Android SDK in this
sandbox); CI is the gate for `:app`/`:core-data` compilation and Robolectric tests.

**Decisions.**
- No Hilt: the graph has 2 singletons; Hilt's generated code + dispatcher would cost APK bytes and
  build complexity for nothing. Manual constructor injection documented in AppGraph.kt. Revisit only
  if the graph grows and the APK stays under 4 MiB.
- Kotlin 2.1.21 + KSP 2.1.21-2.0.1 + Room 2.7.1 (KSP2). DataStore 1.1.1, Compose BOM 2025.05.00.
- AGP 8.10.1 chosen because it supports compileSdk 36.
- `lint.checkReleaseBuilds=false` so release assembly stays deterministic; lint is not part of DoD.
- Dependencies added in this phase: AGP/Kotlin/Compose/M3, Room, DataStore, splashscreen, Truth,
  Robolectric (test-only) — all standard, no third-party code shipped in the APK.
- The Phase −1 push necessarily runs CI against a repo with no Gradle build yet; that run is expected
  red. CI must be green from this commit onward.

**Open questions.** None.

**Next task ID.** 1 (engine).
