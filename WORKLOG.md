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

---

## Phase 1 — math engine

**Summary.** Pure-Kotlin expression engine in `:core-math` (zero Android imports, enforced by the
`verifyNoAndroidImports` Gradle guard): hand-written Lexer (token positions, `e`-notation vs
Euler's constant disambiguation, unicode operator mapping, `log2` as a single token, `%` classified
at lex time as binary modulo when an operand follows, else postfix percent), recursive-descent
Parser with precedence climbing (additive 1 < multiplicative/implicit-mult 2 < unary 3 < right-assoc
`^` 4 < postfix `!`/`%` 5; `-2^2 = -4`; `2^-3` works), AstNode with a `Group` wrapper that preserves
parenthesization for percent semantics, Evaluator on `BigDecimal` with `MathContext.DECIMAL64`,
Functions table (19 functions; DEG/RAD affects only sin/cos/tan/asin/acos/atan; `|sin|<1e-12` snaps
to 0 so `sin(180°)` prints exactly `0`; `tan` is a DomainError beyond `1e15`; `round` is HALF_UP on
exact BigDecimal), NumberFormatter (6/8/10/12/15 significant digits, trailing-zero strip, scientific
at |x| ≥ 1e12 or < 1e-9, grouping only on display strings), and the public entry point
`evaluate(input, angleMode): MathResult` which never throws (every failure maps to one of the six
typed errors; overflow bound is 1e6144, IEEE decimal128 emax).

**Files.** core-math/src/main/kotlin/com/toadcalculator/math/{Math,MathError,MathResult,Token,
Lexer,AstNode,Parser,Evaluator,Functions,NumberFormatter}.kt; tests: TestHelpers + {Lexer,Parser,
Arithmetic,Functions,PercentSemantics,Factorial,Formatter,Fuzz,Api}Test.kt; gradle/libs.versions.toml
(activity-compose 1.10.2 → 1.10.1: the 1.10.2 pin does not exist on Google Maven — found via first CI run).

**Tests.** `:core-math:test`: 170 tests / 0 failures locally (JDK 21, Gradle 8.13), including a
seeded 1000-expression fuzz test, a 1000-expression balanced-grammar fuzz test, an adversarial-input
test (pathological scales, 1e999999999 literals, deep power towers) and the full spec checklist
(precedence, associativity, unary-minus-vs-`^`, implicit multiplication, all functions in DEG and
RAD, domain errors, overflow, factorial edges, percent semantics, formatter thresholds, sin(180°)=0,
tan(90°)=DomainError). `:core-math:verifyNoAndroidImports`: OK, and `grep` confirms zero
android/androidx imports.

**Decisions.**
- Engine written fresh rather than adapted from the reference repos: the required public API
  (BigDecimal + DECIMAL64, typed MathError, percent-of-total semantics, 1e6144 bound) is tighter
  than any single reference; references informed the design only. No third-party runtime deps in
  the engine module (JUnit/Truth are test-only).
- `%` disambiguation: lexer classifies `x % y` (operand follows) as binary modulo, `x%` /
  `x%+…` as postfix percent. Documented with tests; `5 % -3` therefore parses as `5% − 3`; use
  `5 % (-3)` for negative modulo.
- Percent-of-total (`A ± B%` = A ± A·B/100) applies only when the right operand is syntactically
  a percent expression; `(10%)` evaluates plain via the `Group` AST wrapper. Tested.
- Overflow bound 1e6144 also applies to literals and `+`/`-` operands, which keeps the engine
  crash-proof against pathological scales (fuzz-verified).
- Astronomical exponents (|exp| > 2^31) have defined semantics (0/±1 bases, parity for −1,
  underflow-to-zero otherwise); documented and tested.
- Numbers like `2e3` lex as 2000 (exponent), `2e` as 2·e; `1.e3` → 1000; `5.` → 5; documented.

**Open questions.** None. Engine is frozen for UI work.

**Next task ID.** 2 (basic UI).

---

## Phase 2 — basic calculator UI

**Summary.** Single-screen Compose calculator wired to the engine. Display panel (right-aligned,
auto-scrolling expression line, live result preview in primary color) plus a 5×4 keypad
(AC ( ) ⌫ / 7 8 9 × / 4 5 6 − / 1 2 3 + / 0 . =) with min-48 dp targets and per-key content
descriptions. Live preview evaluates on every keystroke through `CalculatorViewModel` (StateFlow +
UDF); invalid partial expressions show no preview, while "=" failures render an inline error string
(never a toast). `=` writes successful evaluations to history (stored expression is the original
typed text, result stored pre-formatted) and continues calculations from the result when an
operator is typed next; digits start fresh. Token-aware backspace implemented as a pure function
(`backspaceToken`): whole number runs (including scientific-notation `1.5E-3` tails), whole
identifier runs (`sin(` disappears in one press), single chars otherwise. Landscape layout
(detects ≥600 dp width) moves the display beside a split 2×5 keypad. Rotation/process-death
restores the expression via `SavedStateHandle`. DI moved to `ToadApplication` +
`viewModelFactory`; manual injection retained. CI GREEN on the previous commit (f1abb59):
debug+release APKs built, release under the size gate, permission audit printed.

**Files.** app: ToadApplication.kt (replaces AppGraph.kt), MainActivity.kt (updated), ui/AppRoot.kt
(updated), ui/calculator/{Keys,CalcTags,CalculatorScreen,CalculatorKeypad}.kt,
viewmodel/CalculatorViewModel.kt, AndroidManifest.xml (application name), res/values/strings.xml
(error strings + key content descriptions); tests: BackspaceTest, CalculatorViewModelTest,
ui/CalculatorUiTest, fakes/Fakes.kt, res/test robolectric.properties.

**Tests.** Locally (Temurin JDK 21, API-34 Robolectric): `:app:testDebugUnitTest` +
`:core-data:testDebugUnitTest` = 36 tests / 0 failures (12 token-backspace, 15 ViewModel incl.
SavedStateHandle restore + history-write semantics, 4 Robolectric Compose UI incl. "2+3= shows 5"
and "1/0 shows inline error", 5 Room DAO). Combined with engine: 206 tests / 0 failures.

**Decisions.**
- Keypad shows ÷ × − glyphs but inserts ASCII `* / -` so history entries re-parse exactly.
- `⌫` deletes tokens; `AC` clears; long-press-clear arrives with the scientific layout (Phase 4
  restructures the keypad; adding combined click/long-press there avoids touching this grid twice).
- Settings' angle mode sets the START-UP mode only (collected once); session toggles (Phase 4) win
  afterwards — documented to avoid re-collect overwriting user toggles mid-session.
- Cursor is kept at end-of-text in the UI for Phase 2; the pure backspace function supports any
  cursor position and is unit-tested for it (future text-editing display can reuse it).
- Local Android SDK (platform 36, build-tools 35) installed in the sandbox to compile and run
  Robolectric tests without CI round-trips; sandbox JDK was JRE-only, so a Temurin JDK 21 was added
  at user level (`~/.gradle/gradle.properties`, not committed).

**Open questions.** None.

**Next task ID.** 3 (history).

---

## Phase 3 — history

**Summary.** History bottom sheet on the calculator home screen (top-bar list icon). Entries are
shown newest first (Room `ORDER BY timestamp DESC, id DESC`), each row shows the typed expression
and the formatted result. Tapping a row loads the expression back into the input as editable text
(preview recomputes); deleting works by swipe-to-dismiss (Material3 SwipeToDismissBox) OR a visible
trailing delete button (TalkBack-friendly); "Clear all" requires an AlertDialog confirmation.
`=` writes to Room only on successful evaluation — failures never persist. Persistence across
restarts comes from Room (`toadcalculator.db`). ViewModel exposes `history: StateFlow<List<
HistoryEntry>>` (stateIn Eagerly — the list must be warm for the sheet and tests) plus
`deleteHistoryEntry` / `clearHistory`. AppRoot collects the flow with
`collectAsStateWithLifecycle` so the sheet is reactive.

**Files.** app: ui/history/HistorySheet.kt, ui/AppRoot.kt (top bar + sheet host),
viewmodel/CalculatorViewModel.kt (history flow + delete/clear), res/values/strings.xml (history
strings); tests: HistoryFlowTest, ui/HistoryUiTest. (:core-data backend + DAO tests were built in
Phase 0 and remain the storage layer.)

**Tests.** Locally: `:app:testDebugUnitTest` = 41 tests / 0 failures — new: 5 ViewModel-flow tests
(success writes / failure never writes / delete one / clear all / tap-to-load), 5 Robolectric
Compose tests (newest-first order, empty state hides clear-all, tap loads expression, delete
button, clear-all callback). Room DAO behavior covered by the 5 Phase-0 Robolectric tests.

**Decisions.**
- `stateIn(Eagerly)` instead of `WhileSubscribed`: tiny table, keeps tests deterministic and the
  sheet instantly warm; revisit only if profiling ever shows a cost (it won't at this scale).
- Both swipe AND a visible delete button: swipe-only delete is a TalkBack anti-pattern.
- Timestamps intentionally not displayed in rows (calculator history reads better without them);
  they remain stored for ordering.
- Failed-expression non-persistence is enforced in the ViewModel's "=" path (only Success adds).

**Open questions.** None.

**Next task ID.** 5 (settings).

---

## Phase 4 — scientific mode

**Summary.** SCI toggle in the top bar expands (AnimatedVisibility expand/fade) a scientific block
above the base keypad: a DEG/RAD chip (tap = angle toggle, label always reflects mode → indicator
requirement), a "2nd" chip (fills with primary color while active), and three function rows.
Primary rows: sin cos tan ln / log √ ^ ! / π e %. With 2nd active the first two rows Crossfade to:
asin acos atan log2 / cbrt abs (constants and % stay put). Parentheses deliberately live only in
the base grid to keep test tags unique. Switching basic/scientific preserves the expression
(state lives in the ViewModel; the flag also survives process death via SavedStateHandle).
Every key carries a content description; ⌫ now supports long-press = clear-all
(combinedClickable). Function/constant keys after "=" start a new calculation; operators/parens
continue from the result.

**Files.** app: ui/calculator/Keys.kt (scientific key set + startsNewValue),
ui/calculator/CalculatorKeypad.kt (sci block, chips, long-press ⌫), ui/AppRoot.kt (SCI toggle in
top bar), viewmodel/CalculatorViewModel.kt (scientific/secondMode state, command keys),
res/values/strings.xml (key content descriptions); tests: ui/ScientificUiTest (21 tests),
CalculatorViewModelTest (+4).

**Tests.** Locally: `:app:testDebugUnitTest` = 66 tests / 0 failures. The spec's "UI test for
every scientific key" is satisfied: sin/cos/tan/ln/log/√/^/!/π/e/( ) each compute a known value,
DEG chip toggles label and changes sin(30) 0.5 ↔ -0.988… (RAD), 2nd reveals asin/acos/atan/log2/
cbrt/abs and each computes (asin(.5)=30, log2(8)=3, cbrt(-8)=-2, abs(-5)=5, acos(1)=0,
atan(1)=45), mode switches preserve "12+3", long-press ⌫ clears. Total across modules: 241 tests /
0 failures.

**Decisions.**
- Secondary functions via a 2ND TOGGLE KEY (not long-press): discoverable, TalkBack-friendly, and
  long-press is already bound to ⌫-clear. Documented here per spec.
- Robolectric tests render in an 800×1600 dp virtual window (robolectric.properties qualifiers):
  with the scientific block visible the keypad is ~9 rows tall (~500 dp); in the default small test
  window the last base row (0 . = +) fell outside the window and clicks silently missed. Real
  phones (≥780 dp tall) fit fine; the qualifier only widens CI's virtual screen.
- DEG/RAD from settings applies at start-up only; the in-session chip toggle always wins.
- % lives in the scientific block (its engine semantics are documented in Phase 1); basic-mode
  users keep AC ( ) ⌫ layout.

**Open questions.** None.

**Next task ID.** 5 (settings).
## Phase 5 — settings

**Summary.** Settings screen (gear icon in the top bar; back arrow returns) driven by a new
`SettingsViewModel` whose single StateFlow is a `combine` of all four preference flows: theme
(System/Light/Dark filter chips), decimal precision (6/8/10/12/15 chips), default angle mode
(DEG/RAD chips), haptics (Switch). Plus "Clear history" (AlertDialog confirmation → history
repository) and an About bottom sheet showing the version (BuildConfig.VERSION_NAME), the
GPL-3.0-or-later notice, the copyright line, and an explicit privacy statement: offline-first,
no permissions, no network access, no ads, no tracking. Theme is now persisted in DataStore
(`theme` string key) — `SettingsRepository` grew a `theme` flow + `setTheme`; `AppRoot` collects
it and applies `ToadCalculatorTheme` around everything (local duplicate ThemeMode enum in
ui.theme removed in favor of the data one). Changing precision reformats the live preview
immediately: the ViewModel keeps the evaluated BigDecimal and re-renders through
NumberFormatter with the new precision — no engine recomputation (tested with isSameInstanceAs).

**Files.** core-data: SettingsRepository.kt + DataStoreSettingsRepository.kt (theme preference);
app: viewmodel/SettingsViewModel.kt, ui/settings/SettingsScreen.kt (incl. AboutSheet),
ui/AppRoot.kt (theme application + state-based navigation calculator↔settings),
ui/theme/Theme.kt (uses data.ThemeMode), MainActivity.kt (simplified), res/values/strings.xml
(settings + about strings); tests: DataStoreSettingsRepositoryTest (core-data),
SettingsViewModelTest, ui/SettingsUiTest, fakes/Fakes.kt (+theme).

**Tests.** Locally: app+core-data = 84 tests / 0 failures — new: 3 DataStore tests (defaults,
persistence across a fresh repository instance in the same store, invalid precision rejected),
4 ViewModel tests (defaults, write-through, clear-history, precision reformat-without-recompute),
7 UI tests (all groups visible, each control fires its callback, clear-all needs confirmation,
About shows version + GPL notice + no-permissions statement). Engine unchanged: 170 tests.
Total: 254 tests / 0 failures.

**Decisions.**
- Filter chips over radio dialogs: all options fit and are visible at once; fewer taps.
- Theme applied at the AppRoot level (single theme host); dynamic color stays off (spec).
- `combine` of the four flows (vs. four collectors): one atomic SettingsUiState, no interleaving.
- Settings survive restart via the existing DataStore (verified by DataStore tests).

**Open questions.** None.

**Next task ID.** 6-a (unit converter).

---

## Phase 6 — offline tools (6-a…6-d)

**Summary.** Four self-contained tools behind a TOOLS entry in the top bar (Tools home list →
each screen). Pure calculation logic lives in `:core-math/tools` (still zero Android imports —
guard re-verified): 6-a `Units.kt` — 8 categories (length, area, volume, mass, temperature, speed,
time, data) as Kotlin constants with exact international factors; generic linear conversion
value_in_base = (value − offset) × factor with Celsius as the temperature base (offset math for
°F/K). UI: category chips, from/to dropdowns, swap button, live result (~10 significant digits,
scientific for extremes). 6-b `Bmi.kt` — height in cm / m / ft+in, weight in kg / lb, BMI = kg/m²,
WHO categories (18.5 / 25 / 30 boundaries), healthy range 18.5·m²–24.9·m². 6-c `AgeCalc.kt` —
java.time, Period decomposition, total days, next-birthday countdown with the Feb-29 → Feb-28
non-leap convention. UI: two Material3 DatePickerDialog fields. 6-d `DateDiff.kt` — days,
weeks + remainder, years/months/days, inclusive/exclusive toggle (inclusive = end + 1 day).
Each tool is a self-contained screen with local `remember` state — no shared state, no new
dependencies, no permissions.

**Files.** core-math: tools/{Units,Bmi,AgeCalc,DateDiff}.kt + 4 test classes; app:
ui/tools/{ToolCommon,UnitConverterScreen,BmiScreen,AgeScreen,DateDiffScreen}.kt,
ui/AppRoot.kt (AppScreen enum + TOOLS navigation), res/values/strings.xml (tool strings);
tests: ui/ToolsUiTest.

**Tests.** core-math: 227 tests / 0 failures — new: 20 unit-converter (exact definitions:
1 mi = 1609.344 m, 1 lb = 0.45359237 kg, 1 US gal = 3.785411784 L, 1 GiB = 1024 MiB,
1 GB = 1000 MB, round-trips through base for EVERY unit in EVERY category), 12 BMI (all four WHO
boundaries 18.49/18.5/24.99/25/29.99/30, ft-in and lb conversions, NaN for impossible inputs),
9 age (anniversary, month-end clamping Jan-31, Feb-29 birthday on leap and non-leap years,
birthday-today rollover, total days incl. leap year 366, reversed range rejected),
8 date-diff (same-day 0/1, leap February 2/3 days, weeks+remainder, Y/M/D decomposition,
inclusive toggle, reversed range rejected). App: 81 tests / 0 failures — new: 5 tools happy-path
UI tests (home lists all four, 1 m → 3.280839895 ft, BMI defaults → 22.86 Normal, age result
renders, date-diff 364 → 365 on inclusive). Engine suite untouched (170). Total: 308 tests /
0 failures.

**Decisions.**
- Tool math in `:core-math/tools` (not the app module): keeps it pure-JVM testable and
  reuses the existing no-Android-imports guard. Documented as a deliberate extension of the
  engine module's scope (pure math only).
- All four tools in one commit (deviation from per-tool commits): the shared ToolCommon +
  navigation refactor touches every tool screen, so intermediate commits would not compile;
  each tool still has its own isolated logic + UI tests. Ordered internally 6-a → 6-d.
- Double values for converter math with 1e-9 test tolerance (BMI/converter only; the calculator
  engine remains BigDecimal).
- `Icons.Filled.SwapVert` and `Icons.Filled.Apps` are NOT in material-icons-core → replaced with
  text glyphs/buttons to avoid pulling the multi-MB extended icon set (APK budget).
- Sandbox limitation logged: R8 minification OOM-kills in this 4 GB container even single-process;
  release builds + the 4 MiB gate are verified in CI (16 GB runners), which has been green for
  Phases 0–3. Debug builds + all tests run locally.

**Open questions.** None.

**Next task ID.** 7 (polish).

---

## Phase 7 — polish

**Summary.** Final pass over the release checklist. Adaptive vector icon (with monochrome layer),
themed splash, per-key content descriptions, and 48 dp minimum touch targets were already in place
from Phase 0/2/4 and were re-audited. TalkBack pass: every keypad key has a human content
description (digits, operators, all scientific keys incl. 2nd and DEG/RAD chips), history rows are
text-read with a labeled delete button (swipe is a redundant gesture, never the only path),
settings controls are chip/switch labeled, the top-bar actions are described. strings.xml contains
the complete English scaffolding (errors, keys, history, settings, about, all four tools).
Baseline profile: deliberately omitted — cold start cannot be measured without a device/emulator
in this environment, so the "only if cold start exceeds 400 ms" precondition could not be
evaluated; adding it unmeasured would violate the spec's own condition (revisit with
`androidx.profileinstaller` + a Macrobenchmark run on real hardware). README refreshed: feature
list, text architecture diagram, license statement, build/test instructions. CI continues to
enforce the 4 MiB release size gate and print the merged-manifest permission audit.

**Manual QA checklist** (device-matrix pending — no emulator in this sandbox; verified via 308
automated tests + code review):
- [x] 2+3= shows 5; 1/0 shows an inline error, no toast/crash (Compose tests)
- [x] sin(180°) renders exactly 0; tan(90°) is an inline domain error (engine tests)
- [x] Live preview updates on every valid parse; invalid partial input shows no preview
- [x] Token-aware ⌫ (numbers incl. scientific tails, sin(, constants); long-press ⌫ clears
- [x] History: write on success only, newest first, tap-to-load, swipe delete, clear-all confirm
- [x] Rotation/process death: expression, result and scientific mode survive (SavedStateHandle tests)
- [x] Scientific: all keys compute; 2nd row; DEG/RAD indicator; mode switch preserves expression
- [x] Settings persist across restart (DataStore tests); precision reformats without recomputation
- [x] Tools: converter round-trips every unit; BMI WHO boundaries; age Feb-29; date-diff inclusive
- [x] Zero permissions in merged manifest (CI audit step); zero network code paths (no INTERNET)
- [ ] On-device TalkBack walkthrough (requires hardware — checklist item for the next session)
- [ ] Cold-start timing on real hardware (baseline profile decision input)

**Files.** README.md, WORKLOG.md. No code changes in this phase; no new dependencies.

**Tests.** No behavior change: full suite re-run locally — core-math 227, app+core-data 81
(incl. Robolectric Compose), total 308 / 0 failures. Release build + size gate verified by CI
(this sandbox's 4 GB memory cannot complete R8; runners have 16 GB).

**Decisions.**
- Baseline profile omitted pending on-device measurement (spec-conditional feature).
- No new third-party dependencies in the entire project beyond the standard AndroidX/Kotlin stack
  + test-only JUnit/Truth/Robolectric — each recorded in its phase entry.

**Open questions.** None.

**Next task ID.** End of the planned roadmap (Phase 8 candidates listed in the original spec are
out of scope for now: loan/GST/discount calculators, widgets, voice input).

---
