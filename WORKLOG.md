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

**Next task ID.** 4 (scientific mode).

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
