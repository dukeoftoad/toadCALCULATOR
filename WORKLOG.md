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
## Phase 9 — UI/UX refinement + web edition removal — 2026-11-06T17:00:00Z

**Summary.** Modernized the calculator's look and feel and removed the out-of-scope
web edition. The visible defects from on-device use (screenshot provided by the
owner): keypad labels rendered at the top-start edge of keys instead of centered;
digit and operator keys visually indistinguishable (both mid-olive); harsh dark-red
AC key; top-bar title "toadCALCULATOR" wrapping to two lines with the SCI/TOOLS
text buttons clipped; after `=` the result appeared twice (big expression line +
preview line); dead, unstructured space above the display.

Changes, by root cause:
- **Centered key labels.** `KeypadButton` now places its label in a
  `Box(contentAlignment = Center)` filling the key — digits and glyphs are
  horizontally/vertically centered (previously top-start).
- **Four-tier key hierarchy** using standard Material 3 surface roles only (no new
  tokens, no new dependencies): digits = `surfaceContainerHigh` (elevated neutral),
  operator column = `primaryContainer` (green-tinted), row-1 keys = `surfaceContainer`,
  AC = softened `errorContainer` (dark scheme errorContainer re-tuned `#93000A` →
  `#56181C` so "clear" reads as a normal action, not a destructive warning),
  `=` = filled `primary`. Scientific keys sit on `surfaceContainerLow` (quiet tier).
- **Keypad geometry.** Uniform 56 dp base keys / 50 dp scientific keys (≥48 dp touch
  target preserved), 8 dp gaps, 16 dp corner radii, 12 dp side padding. Subtle
  press-in scale animation (0.93) via interaction-source-driven
  `animateFloatAsState`. Keypad column is now `verticalScroll`-safe so short
  landscape viewports scroll instead of clipping.
- **Top bar.** Brand wordmark rendered as a single-line annotated string
  ("toad" in primary green + "CALCULATOR" bold, 17 sp, maxLines = 1) — no wrapping.
  SCI/TOOLS text buttons replaced: SCI became a compact 38 dp pill (filled when
  active), TOOLS became an icon button (`Icons.Filled.Build`). Settings and history
  icon buttons unchanged. All test tags and content descriptions preserved.
- **Display.** Single-line expression (was: wrap to 2 lines), auto-shrinking font
  (46 → 38 → 32 → 26 sp by length), auto-scroll keeps the caret end visible.
  After `=` the result becomes the main line, tagged `RESULT` and rendered in the
  primary color — the duplicate preview line is suppressed (previously the number
  showed twice). Tabular figures (`tnum`) on display styles so digits do not
  jitter while typing. Empty state shows a dimmed "0". Long-press anywhere on the
  display copies the current value to the clipboard with a toast confirmation.
- **Scientific grid.** Last row previously had an empty 4th slot (π e % only).
  Added `KeypadKey.Square` ("x²", inserts `^2` — uses the existing power operator,
  zero engine changes) so the constants row is full: π e % x². 2nd-mode rows now
  swap only the function rows while `^ ! π e % x²` stay put (Casio-style
  consistency). Secondary keys get display-only labels (sin⁻¹, cos⁻¹, tan⁻¹,
  log₂, ∛, |x|); inserted text stays ASCII-parseable. 2nd/DEG chips restyled as
  48 dp pill toggles (DEG keeps tertiary container; 2nd fills primary when active).
- **Theme.** Dark background deepened `#12140E` → `#0C0E09` (OLED-friendly);
  primary brightened `#9CD67D` → `#A8DE7B`; full `surfaceContainer*` /
  `outlineVariant` / `inverse*` roles now set explicitly for both schemes.
- **Web edition removed.** `web/` (Next.js prototype, added in the previous
  commit) deleted per owner decision: the product is the Android APK only.

**Files.** `app/src/main/java/com/toadcalculator/ui/theme/Color.kt`,
`Theme.kt`, `Type.kt`,
`app/src/main/java/com/toadcalculator/ui/calculator/CalculatorKeypad.kt` (rewrite),
`CalculatorScreen.kt` (display rewrite), `Keys.kt` (x² + display labels),
`app/src/main/java/com/toadcalculator/ui/AppRoot.kt` (top bar),
`app/src/main/res/values/strings.xml` (cd_key_square, copied_toast),
`app/src/test/java/com/toadcalculator/ui/ScientificUiTest.kt` (x² test),
`WORKLOG.md`, README.md (status line). Deleted: `web/`.

**Tests.** Added `square_key_computes_and_precedence_holds` (5 x² = 25;
3 + 4 x² = 19 — power precedence through the UI). Local sandbox run: core-math
227/227 green (:core-math:test); remaining module suites (:core-data,
:app incl. Robolectric Compose) verified by CI for this commit — the sandbox
JRE-only toolchain + throttled Maven Central made the full local run impractical,
per owner instruction CI is the build/test authority. No existing assertion
changed.

**Decisions.**
- `=` moved to the bottom-right corner (row: 0 . + =), matching iOS/Google/
  Samsung convention; the old layout (0 . = +) left the equals key mid-row.
- After-`=` duplicate result removed by re-tagging the main line as `RESULT`
  while `justEvaluated` — chosen so all existing `assertResult` UI assertions
  keep passing unchanged.
- AC softened rather than keeping M3's harsh dark `errorContainer`: clear-all is
  a frequent, non-destructive action on a calculator.
- x² added to fill the scientific grid hole; strictly a UI-composition of the
  existing `^` operator, so no engine/parser changes or new tests there.
- Web edition removal: owner instruction "delete web version, we are only
  building apk for android".

**Open questions.** None.

**Next task ID.** Owner-directed iteration loop (refine → verify → repeat) on
real hardware feedback.

---
## Phase 9.1 — CI recovery: display semantics fix — 2026-11-06T17:45:00Z

**Skill:** implement — iterate (CI red → root-cause → fix → verify green).

CI run 42258213761 (commit 85765af) failed `:app:testDebugUnitTest` with 24
AssertionErrors: every test touching the display tags
(`EXPRESSION` / `RESULT` / `ERROR`) — all of `CalculatorUiTest` plus 20 of 22
`ScientificUiTest` cases. Chip- and key-only tests stayed green, isolating the
regression to the Phase 9 display refactor.

**Root cause.** The refined `DisplayPanel` wrapped the whole display Column in
`.combinedClickable(onClick={}, onLongClick=copy)`. Clickable-family modifiers
attach `semantics { mergeDescendants = true }`, so the display's tagged `Text`
nodes got folded into the Column's merged semantics node: `onNodeWithTag` on
the merged tree lost them ("could not find any node that satisfies: TestTag =
'expression_display'; however, the unmerged tree contains '1' node that
matches") and visibility/bounds assertions degraded ("The component is not
displayed!"). Reproduced locally after bootstrapping a JDK 21 + Android SDK 36
toolchain in the sandbox (SDK present at ~/android-sdk; Maven Central 429s on
the shared IP worked around with a user-level Aliyun mirror init script +
pre-cached Robolectric android-all jars — none of it committed).

**Fix.** Replaced the display's `combinedClickable` with a
`pointerInput + detectTapGestures(onLongPress = …)` — identical long-press
copy-to-clipboard gesture, zero semantics merging, test tags visible again.
One file: `CalculatorScreen.kt`. No test changed; no behavior change for users
beyond keeping the feature working.

**Verification (local, full CI parity).**
- `:core-math:test :core-data:testDebugUnitTest :app:testDebugUnitTest` →
  BUILD SUCCESSFUL (previously 24 failed).
- `:app:assembleRelease` → 1,729,336 bytes (< 4 MiB limit, R8 green).
- `:app:assembleDebug` → green.
- `aapt2 dump permissions` on release APK → unchanged (androidx-internal
  DYNAMIC_RECEIVER_NOT_EXPORTED only; no network/device permissions).

**Decisions.** Kept long-press copy (owner-approved Phase 9 feature) but via a
raw gesture detector so semantics stay pristine; comment in code documents why
clickable must not return there.

**Open questions.** None.

**Next.** Push → CI run on this commit; owner asked for the failed build to be
re-run — this commit is that re-run (CI triggers on push to main).

---
## Phase 9.2 — Tools home redesign — 2026-11-07T02:04:00Z

**Skill:** implement — iterate (owner screenshot feedback: Tools landing page is generic).

Owner sent a real-device screenshot of the Tools home: a bare `ListItem` stack with
hairline dividers, no icons, no tappable affordance, no hierarchy (title and subtitle
near-identical weight), and a large dead region below the last row. This is the page
that had never received the Phase 9 visual pass the calculator got.

**Redesign.**
- Four tonal cards (`Surface`, shape 20dp, `surfaceContainer` on background — visible
  in both schemes without a border) replace the divider list. Row padding 16/14dp,
  12dp gaps, 16dp screen insets; content is vertically centered per card.
- Leading icon tile: 46dp rounded-square (14dp corners) filled `primaryContainer`
  with the icon tinted `onPrimaryContainer` — the single green accent of the keypad
  carried into navigation.
- Custom stroke-style vector icons (24dp, 1.8dp round strokes) drawn per tool:
  opposing arrows (converter), heart (BMI), cake with candle (age), calendar with
  day dots (date difference). Hand-authored paths, no `material-icons-extended`
  dependency — release size stays untouched.
- Trailing `Icons.AutoMirrored.Filled.KeyboardArrowRight` in `outline` as the tap
  affordance (existence verified against the actual BOM-mapped icons-core AAR).
- Short intro line (`tools_subtitle`) under the bar and a centered brand footnote
  (`tools_footnote`: "Offline-first · no ads · no tracking") fill the dead space
  without inventing content.
- Staggered entrance: one shared `Animatable` drives all four cards through
  `graphicsLayer` alpha/translationY windows (index*0.08 offset, 380ms,
  FastOutSlowInEasing). Finite duration → Robolectric idle-waiting is unaffected;
  alpha/translation never touch semantics, so test tags stay visible (Phase 9.1
  lesson respected — no clickable/combinedClickable near tagged children; the tag
  lives on the clickable Surface node itself).

**Files.** `app/src/main/java/com/toadcalculator/ui/tools/ToolCommon.kt`
(ToolsHomeScreen rewrite + `toolCardTag`/`toolIconRes` helpers),
`app/src/main/res/drawable/ic_tool_{convert,bmi,age,datadiff}.xml` (new),
`app/src/main/res/values/strings.xml` (tools_subtitle, tools_footnote),
`app/src/test/java/com/toadcalculator/ui/ToolsUiTest.kt` (new interaction test).

**Tests.** Added `tools_home_cards_open_destination` — clicking the BMI card sets
`onOpen(BMI)`, clicking the date-difference card sets `onOpen(DATE_DIFF)` (first
navigation-callback coverage for this screen). Existing suite unchanged, including
`tools_home_lists_all_four` (all four titles remain visible; texts now live in
merged Surface semantics, which `onNodeWithText` matches). Sandbox toolchain was
wiped between sessions (no local gradle/SDK this time); per standing owner
instruction CI is the build/test authority for this commit — full module suite,
assembleRelease (< 4 MiB gate) and the permissions audit run on push.

**Decisions.**
- Single accent color for all four tiles (not per-tool colors): keeps the toad-green
  brand unified, matches the keypad's one-filled-accent hierarchy.
- Vertical full-width cards instead of a 2×2 grid: subtitles are long (converter
  lists eight domains) and would truncate/underfit in half-width cells.
- Dots on the calendar icon drawn as filled arc-circles, frame as strokes — verified
  path geometry by hand; icons render through Compose tint (ColorFilter), so the
  drawable's own white stroke color is irrelevant.
- `tool_open_*` test tags preserved verbatim via `toolCardTag()` helper; no
  AppRoot/navigation changes needed (same `onOpen` contract).

**Open questions.** None.

**Next.** Push → CI; continue iterate loop on owner hardware feedback (other
screens — settings, history sheet — may deserve the same pass).
## Phase 9.3 — toad identity, audit fixes, signed release, back-gesture bug — 2026-11-07T03:10:00Z

**Skill:** implement — iterate (owner: "Craft toad logo, toad icon and other feature for
signature of toad. Fix the flaws you have found during audit, release signed apk, swipe
back from converter got closed instead of coming to back home tab fix it").

**Bug (back gesture).** Root navigation is plain state (`screen`, `activeTool`) — no
`BackHandler` was ever registered, so the Android system-back gesture / button from any
non-calculator screen finished the activity instead of drilling back. Fix:
`BackHandler(enabled = screen != CALCULATOR)` in AppScaffold — tool → tools home →
calculator, mirroring the top-bar arrow; at the calculator root the handler is disabled
so default exit still applies. Manifest gained `android:enableOnBackInvokedCallback="true"`
(targetSdk 36 predictive back goes through the same androidx dispatcher). Regression
cover: new `AppRootUiTest` composes the real AppRoot (Room/DataStore on Robolectric via
the app graph) and drives the actual `onBackPressedDispatcher` — converter → back →
tools home, tools home → back → calculator.

**Toad identity (hand-authored vectors, no binary assets).**
- `ic_launcher_foreground.xml` replaced with the toad: crowned head, two eye bumps,
  wide cheeks, front feet, big smile — geometry hand-computed to stay inside the 66dp
  adaptive safe zone (all points ≤ ~26dp from center 54,54). White body + #1B2A13
  details on the `ic_launcher_background` green, retuned from #386A20 to the exact
  brand `LightPrimary` #3E6B1E. Same drawable serves as monochrome layer (alpha-based)
  and as the splash animated icon, so launcher/splash/themed icon stay one signature.
- `ic_toad_logo.xml`: standalone bright-green toad mark for in-app use.
- About sheet is now the in-app signature moment: 56dp toad mark over the
  green-`toad`/bold-`CALCULATOR` wordmark, centered, privacy line promoted to bodyMedium.

**Audit fixes.**
- Typography: `titleMedium` (16/24 w500), `bodyMedium` (14/20), `bodySmall` (12/16)
  were referenced by Phase 9.2 UI but never defined — silently falling back to M3
  defaults. Now explicit tokens.
- CI workflow hardcoded `app-release-unsigned.apk` in three places (size gate,
  artifact upload, permissions audit) — would break the moment signing changed the
  output name. Replaced with globbed APK resolution.

**Signed release.**
- Generated `signing/toadcalculator-release.keystore` (RSA 2048, validity 10950 days,
  alias `toadcalculator`, SHA-256 40:9D:43:46:64:77:04:7E:10:F9:3F:09:04:7D:86:DC:1D:
  24:23:B8:A4:DA:65:26:71:12:91:31:E2:A0:D1:82).
- `signing/signing.properties` (store password, alias, key password) sits next to it;
  both committed to this **private** repository so CI and local builds sign identically
  with zero secret plumbing. Trade-off accepted deliberately: for a personal offline
  app, recoverability outranks keystore secrecy; rotate by replacing both files.
- `app/build.gradle.kts` reads the properties and wires `signingConfig` on release
  when present; absent the file the build falls back to unsigned (still green).

**Files.** `app/src/main/res/drawable/ic_launcher_foreground.xml`, `ic_toad_logo.xml`,
`app/src/main/res/values/colors.xml`, `AndroidManifest.xml`,
`app/src/main/java/com/toadcalculator/ui/AppRoot.kt` (BackHandler),
`ui/theme/Type.kt`, `ui/settings/SettingsScreen.kt` (AboutSheet),
`app/build.gradle.kts`, `.github/workflows/build.yml`,
`app/src/test/java/com/toadcalculator/ui/AppRootUiTest.kt` (new),
`signing/*` (new), `WORKLOG.md`, README.md.

**Tests.** Added 2 root-navigation regression tests (the reported bug). No existing
assertion changed. Sandbox has no Android toolchain this session (wiped); per standing
owner instruction CI is the build/test authority for this commit.

**Open questions.** None.

**Next.** After CI green on this commit: tag `v1.0.0`, create the GitHub Release and
upload the signed APK as a release asset.
---
## Phase 9.4 — CI recovery: Kotlin DSL signing-script fix — 2026-11-07T02:55:00Z

**Skill:** implement — iterate (CI red → root-cause → fix → verify green).

CI run 42330755277 (commit c84e54b) failed at script compilation of
`app/build.gradle.kts` — `Unresolved reference: util` at 12:25. Root cause: inside
`.gradle.kts` scripts the bare name `java` resolves to the plugins' `java { }`
extension accessor, not the `java.*` package, so `java.util.Properties()` does not
compile there. Fix: top-of-file `import java.util.Properties` + bare `Properties()`
constructor — the standard Gradle Kotlin DSL pattern. One line of behavior, zero
signing-logic change. No test touched; the suite never ran (script compile is the
first gate).

**Next.** Push fix → CI rerun of the full Phase 9.3 change set.
---
## Phase 9.5 — CI recovery: AboutSheet compile fix — 2026-11-07T03:05:00Z

**Skill:** implement — iterate (CI red → root-cause → fix → verify green).

CI run for 0a75352 cleared the script-compile gate but failed `:app:compileDebugKotlin`
with three errors, all in the Phase 9.3 AboutSheet rework in `SettingsScreen.kt`:
`Unresolved reference 'Icon'` (material3 Icon used without import) and
`withStyle(fontWeight = …)` (named-argument misuse — `withStyle` takes a `SpanStyle`,
which also dragged the `stringResource` inside into a non-composable-context error).
Fix: add `androidx.compose.material3.Icon` import; wrap the second span in
`SpanStyle(fontWeight = FontWeight.Bold)`. No logic change; wordmark renders as
intended (green "toad" + bold "CALCULATOR").

**Next.** Push → CI rerun; then tag v1.0.0 + GitHub Release with the signed APK.
---
## Phase 9.6 — CI recovery: root-nav test rule fix — 2026-11-07T03:15:00Z

**Skill:** implement — iterate (CI red → root-cause → fix → verify green).

CI on 40469f3: app sources now compile; failure moved into the new test —
`AppRootUiTest.kt:39 Unresolved reference 'activity'`. `composeRule.activity` exists
only on `AndroidComposeTestRule` (i.e. `createAndroidComposeRule<T>()`), not on the
plain `ComposeContentTestRule` returned by `createComposeRule()`. Fix: rule swapped to
`createAndroidComposeRule<ComponentActivity>()` (+ `androidx.activity.ComponentActivity`
import); test-manifest dependency already provides the activity under Robolectric.
No assertion change.

**Next.** Push → CI rerun; then tag v1.0.0 + GitHub Release with the signed APK.
---
## Phase 9.7 — v1.0.0 signed release published — 2026-11-07T03:25:00Z

**Skill:** document — release handoff.

CI run 42331605210 (d5bd99c) fully green: all module unit tests (incl. the two new
root-navigation regression tests), debug + signed release assemble, size gate
(`app-release.apk` — signed — at 1,758,280 bytes vs the 4 MiB limit), zero-permission
audit. The presence of `app-release.apk` (vs `app-release-unsigned.apk`) confirms the
signing config applied.

**Published.** Tag `v1.0.0` at d5bd99c; GitHub Release "toadCALCULATOR 1.0.0"
(release id 437284249) with asset `toadCALCULATOR-1.0.0.apk` (1,758,280 bytes,
state uploaded). APK pulled from the green CI run's artifact — identical bytes to
what the gate measured.

**Owner-visible iteration cost this phase.** Three red CI runs before green, all
toolchain-level slips on untested-locally code (Kotlin DSL `java` namespace,
missing material3 Icon import + withStyle misuse, wrong compose test rule). Each
root-caused and logged (9.4–9.6). No product-logic regressions at any point.

**Next.** Iterate on owner hardware feedback: settings/history sheets still carry
the pre-Phase-9 look if the owner wants the same modern pass.
---

## Phase 9.8 — v1.1.0: memory + Ans, percentage tool, display glyphs, modern history/settings, bug round — 2026-11-08T05:40:00Z

**Skill:** implement — iterate (owner: "Prepare next version updates with new features, flaws fixing, bugs fixing if any").

**Summary.** The first post-1.0 feature release. A read-only audit of the v1.0.0
tree surfaced 16 bugs and 21 flaws; this phase ships the subset that fit a single
CI-verified iteration without a local Android toolchain (sandbox has JDK 21 JRE
only — no `javac`, no SDK; CI remains the build/test authority, per standing owner
instruction). Five new features, twelve bug fixes, four flaw fixes, and a modern
visual pass on the two screens Phase 9.7 explicitly flagged.

**New features.**
- **Memory keys + Ans** (N1+N4). A fifth scientific-block row — MC / MR / M+ / M− /
  Ans — folds the live preview into a `BigDecimal` memory store and recalls the
  last successful `=` result. `memory` and `lastAnswer` persist through
  `SavedStateHandle` (as `formatForInput` strings, restored via `BigDecimal(it)`)
  so they survive process death. The display gains "M" and "Ans" status badges
  above the expression when the stores are non-empty. Memory lives in the
  scientific block only so the basic 5×4 keypad — and its viewport budget — is
  untouched.
- **Percentage tool** (N3). A fifth offline tool: four modes (is-what-%-of, % of,
  increase, decrease) over two inputs, pure math in `core-math/tools/Percent.kt`
  (7 JVM tests). Mirrors the existing tool screen pattern; new `ic_tool_percent`
  stroke vector; new `ToolDestination.PERCENT` wired through ToolCommon + AppRoot.
- **Display glyphs** (B9, promoted to a feature). The expression line now renders
  `× ÷ √ ∛` instead of `* / sqrt( cbrt(`; the stored expression stays ASCII so
  history entries re-parse. Applied to the expression line only — the result line
  keeps ASCII `-` so signed numbers and existing result assertions are unchanged.
- **Modern history sheet** (F2). Entries are tonal `surfaceContainer` cards with a
  `primaryContainer` "=" icon tile, `titleMedium` expression + `bodyMedium` result,
  staggered by 10 dp gaps (dividers removed). Empty state gained a centered tile.
- **Modern settings screen** (F2). Each preference is a tonal card (the settings
  analogue of a tool card); the low-contrast `HorizontalDivider`s are gone.

**Bug fixes.**
- B1: history "Clear all" now opens its confirmation dialog — the dialog state
  existed in the wrapper but the button bypassed it, so one tap wiped everything.
  Moved the dialog into `HistorySheetContent`; updated the locked-in test.
- B2: pressing `=` again right after a successful `=` no longer re-evaluates the
  formatted result and logs a redundant "5 → 5" history entry.
- B3: `angleMode` and `secondMode` now persist to `SavedStateHandle` and survive
  process death. The init block no longer overrides a restored session angle-mode
  toggle with the settings default.
- B4: BMI no longer renders a misleading "Underweight" label beneath the "—"
  placeholder when input is empty/invalid — `category` is null for NaN.
- B5: the healthy-weight range guards against height ≤ 0 (was 0.0–0.0 kg).
- B7: the root `BackHandler` predicate now includes `showHistory`, so back from a
  history sheet opened over SETTINGS/TOOLS closes the sheet first instead of
  leaving `showHistory` lingering in saved state.
- B8: `android:allowBackup="false"` — the About sheet promises "your data never
  leaves this device"; Auto Backup to Google Drive would have copied the Room DB
  and DataStore off-device, contradicting that claim.
- B11: unit-converter swap now carries the converted quantity (1 m → 3.28 ft,
  swap → 3.28 ft → 1 m) instead of leaving the raw "1" in the new from-unit.
- B12: swap button gained a `contentDescription` ("Swap units") for TalkBack.
- B13: removed the dead `KeypadKey.AngleToggle -> "DEG"` label branch (the chip
  renders via `AngleChip`, not `KeypadButton`; the branch was both unreachable
  and wrong in RAD).
- B15: `versionCode = 2`, `versionName = "1.1.0"`.
- B16: the 2nd chip and the SCI pill now expose toggle state to TalkBack
  (`role = Role.Switch` + `selected`); previously they announced as plain
  buttons with no on/off state.

**Flaw fixes.**
- F1: SCI pill raised from 38 dp to 48 dp (touch-target minimum).
- F3: removed the dead `dynamicColor` parameter from `ToadCalculatorTheme`
  (never overridden; dynamic color is intentionally off).
- F10: Age and DateDiff screens show an inline error-colored message when dates
  are inverted, instead of silently hiding the result block.
- F2: history + settings modern pass (see above).

**Deferred (documented, not done this phase — each needs either an owner decision
or a local toolchain to de-risk):** B6 (long-press ⌫ fires a framework haptic
even with haptics off — fix requires replacing `combinedClickable` with a custom
`pointerInput` gesture, paired with the N5 haptic-strength rewrite which changes
the `SettingsRepository` interface and breaks many tests), B10 (cold-start theme
flash — touches splash timing), B14 (signing keystore in-repo — explicitly
accepted in 9.3, revisit if distribution widens), F4 (dead `cursor` field — or
wire as the N10 tap-to-edit-cursor feature), F12/F13 (title-size/typography-token
cleanup), F14 (formatter unification), F18 (Build→Calculate icon — needs
core-icon verification), F20 (translations — needs target markets), F21 (lint in
CI), N2 (programmer/hex mode — L-sized, its own phase), N6 (copy/share menu),
N7–N10.

**Files.** `app/build.gradle.kts` (version bump), `AndroidManifest.xml`
(allowBackup), `res/values/strings.xml`, `res/drawable/ic_tool_percent.xml` (new),
`ui/calculator/{Keys,CalcTags,CalculatorKeypad,CalculatorScreen}.kt`,
`ui/AppRoot.kt`, `ui/tools/{ToolCommon,PercentScreen,UnitConverterScreen,BmiScreen,
AgeScreen,DateDiffScreen}.kt` (PercentScreen + ic_tool_percent new),
`ui/history/HistorySheet.kt`, `ui/settings/SettingsScreen.kt`, `ui/theme/Theme.kt`,
`viewmodel/CalculatorViewModel.kt`, `core-math/.../tools/Percent.kt` (new),
`core-math/.../tools/PercentTest.kt` (new), `app/.../CalculatorViewModelTest.kt`,
`app/.../ui/{ScientificUiTest,ToolsUiTest,HistoryUiTest}.kt`, `README.md`,
`WORKLOG.md`. Every source file keeps its SPDX header.

**Tests.** Added 7 pure-JVM `PercentTest` cases; 9 `CalculatorViewModelTest` cases
(double-equals, angle/second/memory state-restore, M+/M−/MR/MC, Ans); 2
`ScientificUiTest` cases (memory store+recall with the on-display indicator, Ans);
1 `ToolsUiTest` case (percentage tool default); renamed
`tools_home_lists_all_four` → `..._all_five` (now asserts Percentage too);
updated `HistoryUiTest.clear_all_*` to assert the confirmation dialog fires
before clearing. No existing assertion weakened; the one behavior change (B1) is
the bug fix itself, and its test now reflects correct behavior. A read-only
compile-safety review of all 23 changed files found no compile-blockers
(exhaustive `when`s, imports, symbol references, string resources, signatures
all verified) — but the sandbox cannot compile, so CI is the final gate.

**Verification.** CI green after the iterate loop (per standing instruction CI
is the build/test authority — sandbox has no Android toolchain):
- Run 42453140405 (f6e4fbd) → red at `:core-math:test`: `PercentTest.increase`
  asserted `isEqualTo(220.0)` but `200*(1+10/100)` drifts to
  220.00000000000003 in Double (0.1/1.1 aren't binary-exact). Fixed by switching
  to binary-exact values `100*(1+50/100)=150.0` (commit 7949b24).
- Run 42453390304 (7949b24) → red at `:app:testDebugUnitTest`: the percent UI
  test's `onNodeWithText("12.5")` uses the exact-match default, but the result
  row renders "12.5 %" (unit appended). Fixed with `substring = true`
  (commit d58085a). Notably all 96 other app tests passed first try — every new
  VM test (memory/Ans/state-restore), both ScientificUiTest cases, the
  HistoryUiTest confirmation update, and both AppRootUiTest back-nav cases.
- Run 42453575638 (d58085a) → **green**: both `build` and `permissions-audit`
  jobs success. Signed `app-release.apk` at **1,766,344 bytes (1.68 MiB)** vs
  the 4 MiB gate. Manifest audit prints only the AGP-auto-injected
  `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (signature-scoped to this app's
  own package — the standard internal-IPC guard present since v1.0.0, not a
  real permission); the audit job is informational and succeeds, as in 9.7.

**Owner-visible iteration cost this phase.** Two red CI runs before green,
both test-only floating-point / matcher slips on code that couldn't be
executed locally (no `javac`/SDK). Each root-caused from the CI logs and fixed
in a single follow-up commit. No product-logic regressions — the 96 other app
tests and all core-math tests passed on the first red run.

**Decisions.**
- Memory keys live in the scientific block, not the base keypad, to preserve the
  viewport budget (the basic `=` key must stay visible on an 844 px viewport —
  see Phase W1 decisions). Basic-mode users get Ans-free history chaining as
  before; scientific-mode users get full physical-calculator parity.
- `displayExpression` swaps only `* / sqrt( cbrt(` — not `-` (unary/binary
  disambiguation is error-prone) and not `+` (already a clean glyph). The win is
  `× ÷ √ ∛`; the stored expression stays ASCII so history reload re-parses.
- The history/settings modern pass uses cards + icon tiles but deliberately
  omits the staggered `Animatable` entrance — the sheet/screen already animates
  on entry, and the stagger adds composition cost + CI risk for marginal gain.
- `allowBackup=false` is the simplest privacy-correct choice; a scoped
  `dataExtractionRules` would allow backup of non-sensitive prefs, but for a
  zero-data-leaving app "off" is the honest default.

**Open questions.** None blocking. The deferred items (esp. B6/N5 haptic strength
and B14 signing) are owner decisions for a future phase.

**Next.** CI is green on d58085a. Tag `v1.1.0` at d58085a and cut a GitHub
Release with the signed `app-release.apk` (artifact `toadCALCULATOR-release`
from run 42453575638, 1,766,344 bytes). Then iterate on owner hardware feedback
— the deferred items (esp. B6/N5 haptic strength and F2 staggered entrances)
are the next candidates.
