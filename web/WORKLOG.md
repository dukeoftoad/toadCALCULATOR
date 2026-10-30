# WORKLOG — toadCALCULATOR (web edition)

Append-only. One entry per phase: summary, files, verification, decisions,
open questions, next task. UTC timestamps. This file is the handover
document for a fresh agent session.

> **Context.** The Android implementation (Phases −1 → 7) lives in the private
> repository `dukeoftoad/toadCALCULATOR` and is complete — see its WORKLOG for
> the full history (engine with 170 tests, Compose UI, Room history, DataStore
> settings, tools, final QA). This sandbox environment renders a Next.js web
> app as the user-visible deliverable, so the same specification was ported to
> the web while preserving every engine semantic and product decision.

---

## Phase W1 — Web port: engine, UI, tools (complete)

**UTC:** 2026-10-30
**Skill:** plan → implement → test → review → document

**Summary.** Ported the toadCALCULATOR spec to Next.js 16 + TypeScript +
Tailwind 4 + shadcn/ui (sandbox requirement). The math engine was re-implemented
in pure TypeScript on decimal.js with a DECIMAL64-analogue context (16
significant digits, ROUND_HALF_EVEN, maxE 6144) so `0.1+0.2 = 0.3`, mirroring
the Android BigDecimal core. All Android product decisions were preserved:
2ND toggle (not long-press) for secondary functions; long-press ⌫ = clear-all;
lex-time `%` classification (binary modulo when an operand follows, else
postfix percent — `5 % -3` parses as `5% − 3`); percent-of-total only when a
raw percent node is the direct right operand of `±` (`200+10% = 220`,
`200+(10%) = 200.1`, `(10%) = 0.1`); 19 functions with DEG/RAD affecting trig
only; `|sin| < 1e-12` snap; 1e6144 overflow bound; formatter thresholds
(|x| ≥ 1e12 or < 1e-9 → ×10ⁿ); WHO BMI boundaries 18.5/25/30 with healthy range
18.5·m²–24.9·m²; exact international unit factors, 1 GB = 1000 MB (+KiB/MiB);
Feb-29 birthday → Mar 1 in non-leap years; inclusive date-diff toggle.

**Files.** src/lib/math/{types,lexer,parser,evaluator,formatter,index}.ts;
src/lib/calc/editing.ts; src/lib/tools/{units,bmi,datetime}.ts;
src/store/calculator.ts (zustand + persist, partialized); src/hooks/use-calc-hooks.ts;
src/components/calculator/{calculator-app,display,keypad,history-sheet,settings-panel}.tsx;
src/components/tools/{tools-view,unit-converter,bmi-calculator,age-calculator,date-diff}.tsx;
src/app/{page,layout,globals.css}; public/icon.svg; README.md; LICENSE (GPL-3.0-or-later).
Every source file carries SPDX + copyright headers.

**Verification.**
- Engine battery (inline script, not committed): 91 checks pass / 0 fail —
  precedence, associativity, `-2^2 = -4`, `2^3^2 = 512`, implicit multiplication,
  e-notation vs Euler (`2e3 = 2000`, `2e = 2·e`), percent semantics, modulo,
  all 19 functions in DEG and RAD, domain errors, `0^0 = Undefined`,
  `10^6145 = Overflow`, factorial edges, formatter thresholds + grouping,
  plus a 1000-expression fuzz run with 0 crashes.
- Browser (agent-browser, desktop 1280×800 + mobile 390×844): 2+3=5;
  1÷0 → inline DivisionByZero (no toast/crash); 200+10% = 220; 200% = 2;
  ans-chaining (×2 → 440); asin(0.5) = 30 via 2ND; RAD chip + sin(π) = 0;
  sin(30) = 0.5 back in DEG; token-aware ⌫ (whole `sin(` removed); long-press
  ⌫ clears all; history sheet rows newest-first with load-back; failed
  expressions excluded from history; precision 6 reformats 1÷3 → 0.333333
  without recompute, 12 restores 0.333333333333; theme toggle; converter
  1 m = 3.280839895 ft + swap + 100 °C = 212 °F; BMI 22.5/22.4 with WHO badge
  and converted healthy ranges; age 2000-01-01 → 26y 9m 29d, next birthday
  countdown; date-diff Jan 1–31 = 30 d exclusive / 31 d inclusive / 1 mo
  calendar span; physical-keyboard 59+1 Enter = 60.
- `bun run lint` clean; dev.log clean (no runtime errors, no hydration
  warnings).

**Decisions.**
- decimal.js (precision 16, half-even) stands in for BigDecimal/DECIMAL64;
  trig/inverse-trig/hyperbolics go through double with exact DEG snapping —
  same strategy as the Android evaluator.
- Scientific pad = 4-column function rows; 2ND swaps sin→sin⁻¹, cos→cos⁻¹,
  tan→tan⁻¹, ln→eˣ(exp), log→log₂, √→∛, !→|x| (abs); π e % stay put.
  Merged SCI/2ND/DEG chips into one toolbar row to keep a phone keypad
  within one viewport (mobile docH 876 px on an 844 px viewport — the `=`
  key is fully visible; footer is mt-auto sticky).
- Persistence via localStorage (zustand persist, versioned key
  `toadcalculator-v1`) is the web analogue of Room + DataStore; keeps the
  app 100 % offline with zero network requests — matching the zero-permission
  requirement.
- Hydration detection via `useSyncExternalStore` (no setState-in-effect);
  today's date likewise, so SSR/client renders stay consistent.
- Result copy uses the Clipboard API with a sonner toast confirmation.

**Open questions.**
- The Android repo's CI (JDK 21, size gate) is unaffected by this web port;
  a web CI workflow can be added if this edition is ever published standalone.

**Next task.** Sync this web edition into `dukeoftoad/toadCALCULATOR`
(`web/` subtree) so both editions share one private home; optional PWA
manifest for installable offline use.
