<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# toadCALCULATOR — Plugin (Extension) Architecture: Implementation Map

Status: **proposal** (owner requested 2026-11-08: "plugin-like features where user
can install extensions that are released in GitHub and in app can scroll the
available plugins and install on their devices"; the worked example is a
**power calculator that recommends an MCB rating from AC/DC inputs**).
This document is the full implementation map. No plugin code is written yet.

---

## 0. The one hard constraint everything is designed around

toadCALCULATOR's identity and its CI gate are **zero permissions**:

- README promise: "Zero permissions, zero ads, zero tracking, zero network access".
- CI `permissions-audit` job asserts the merged manifest has no permissions.
- `allowBackup=false`; data never leaves the device.

A plugin store that fetches GitHub from inside the app needs
`android.permission.INTERNET` — which would **fail the audit, break the privacy
copy, and change the product's meaning**. So the architecture below keeps the
app network-free by construction and moves the network hop to the component
that already has one: **the browser**. The app stays the installer and the
runtime; the browser is the delivery truck.

Everything else follows from that decision.

---

## 1. Design decision: declarative plugins, not dynamic code

A "plugin" could be shipped as compiled code (DEX/APK loaded via
`DexClassLoader`) or as **data**. This map chooses data, deliberately:

| | Declarative bundle (chosen) | DEX code loading (rejected) |
|---|---|---|
| Execution safety | Pure math over `core-math` — no code runs from the plugin | Arbitrary code runs with the app's full privileges |
| Review/audit | The whole plugin is readable JSON in a text editor | Requires reverse-engineering each release |
| Size impact | ~0 (JSON parsed at runtime) | D8 runtime complexity, per-plugin APK weight |
| Authoring skill | Anyone who can write formulas | Kotlin + Android build toolchain |
| R8/ProGuard | Untouched | Reflection/hiding classes from shrinker |
| Fits the 4 MiB gate | Yes | Threatens it |
| Play-policy surface | None (no executable payload) | Dynamic code is a policy-sensitive area |

**A plugin is a calculator tool, and calculator tools in this app are already
declarative at heart**: inputs → pure functions → formatted outputs (see
`ToolCommon.kt`, `core-math/tools/*`). The plugin system generalizes exactly
that shape. What a plugin can never do — by construction, not by policy — is
touch the network, storage, history, or anything else. There are no such
primitives in the runtime to call.

---

## 2. Plugin format: `.toadext`

One file, one plugin. A `.toadext` file is a UTF-8 JSON document (v1 ships
raw JSON; zipping is only worth it if we later embed assets — see §11 Risks):

```
{ "specVersion": 1, "id": "power-mcb-advisor", "version": "1.0.0", ... }
```

- **`specVersion`** — the manifest DSL version. The app accepts
  `specVersion <= SUPPORTED_SPEC` (1) and rejects newer with a clear message;
  that is the forward-compat valve.
- **`id`** — stable slug (`power-mcb-advisor`). Re-importing a file with the
  same `id` **upgrades** (and warns on downgrade); a different `id` installs
  alongside.
- **`version`** — semver-ish display string; comparisons are numeric on the
  registry side.
- **`minAppVersion`** — optional floor ("1.3.0") checked against
  `BuildConfig.VERSION_NAME`.
- Size caps, enforced by the validator (§5): manifest ≤ 256 KB, ≤ 24 inputs,
  ≤ 24 outputs, ≤ 12 tables, each expression ≤ 400 chars. Evaluation is a
  fixed single pass (no loops exist in the DSL), so runtime is trivially
  bounded.

### 2.1 Manifest schema v1 (informative — normative spec lands with the code)

```jsonc
{
  "specVersion": 1,
  "id": "power-mcb-advisor",
  "name": "MCB / breaker advisor",
  "version": "1.0.0",
  "author": "dukeoftoad",
  "description": "Recommends a miniature circuit breaker rating from load, voltage and system type (AC 1-phase, AC 3-phase, DC).",
  "minAppVersion": "1.3.0",

  "icon": "ic_tool_calculator",       // bundled icon registry name, see 4.5
  "accent": "tertiary",               // tile color hint: primary|secondary|tertiary

  "inputs": [
    { "id": "system",   "type": "enum", "label": "System",
      "options": [
        { "value": "ac1", "label": "AC, single phase" },
        { "value": "ac3", "label": "AC, three phase" },
        { "value": "dc",  "label": "DC" }
      ], "default": "ac1" },
    { "id": "voltage",  "type": "number", "label": "Voltage (V)",
      "min": 1, "max": 1000, "required": true },
    { "id": "power",    "type": "number", "label": "Load power (W)",
      "min": 0, "required": false,
      "visibleWhen": "system != 'dc'" },
    { "id": "current",  "type": "number", "label": "Load current (A)",
      "min": 0, "required": false,
      "visibleWhen": "system == 'dc'" },
    { "id": "pf",       "type": "number", "label": "Power factor",
      "min": 0.1, "max": 1, "default": 0.95,
      "visibleWhen": "system != 'dc'" },
    { "id": "loadKind", "type": "enum", "label": "Load character",
      "options": [
        { "value": "resistive", "label": "Resistive (heater, kettle)" },
        { "value": "mixed",     "label": "Mixed / general" },
        { "value": "inductive", "label": "Motor / inductive (inrush)" }
      ], "default": "mixed" }
  ],

  "tables": [
    { "id": "standardRatings",
      "columns": ["rating"],
      "rows": [ [6],[10],[16],[20],[25],[32],[40],[50],[63],[80],[100] ] }
  ],

  "outputs": [
    { "id": "loadCurrent", "label": "Computed load current",
      "expression": "system == 'dc'
            ? if(current > 0, current, 0)
            : power / (voltage * (system == 'ac3' ? 1.7320508 : 1) * pf)",
      "unit": "A", "format": "sig10", "kind": "info" },

    { "id": "mcbRating", "label": "Recommended MCB rating",
      "expression": "pickCeiling(loadCurrent, standardRatings.rating, 1.25)",
      "unit": "A", "format": "fixed1", "kind": "primary" },

    { "id": "curve", "label": "Suggested curve",
      "expression": "loadKind == 'inductive' ? 'C (motor inrush)' : (loadKind == 'resistive' ? 'B (fast trip)' : 'B or C')",
      "kind": "info",
      "condition": "mcbRating > 0" },

    { "id": "marginNote", "label": "Note",
      "expression": "'Chosen with 1.25x headroom over ' + loadCurrent + ' A'",
      "kind": "note" }
  ]
}
```

New DSL constructs over the existing calculator grammar (all evaluated by
`core-math`, none add Turing-completeness):

- **Bindings**: identifiers resolve to input values (`voltage`, `pf`) and to
  earlier outputs (`mcbRating` usable in later `condition` strings) — a
  `bindings: Map<String, BigDecimal>` passed to the evaluator (§4.2). String
  values from enum inputs ride a small string-binding side-channel
  (`system == 'ac3'` compiles to a string comparison, not a math one).
- **`?:` ternary and `if(cond, a, b)`** — one ternary evaluator node, two
  spellings. Both operand branches evaluate eagerly for deterministic,
  total expressions (there are no effects to short-circuit).
- **`pickCeiling(x, table.column, margin)`** — the only table helper v1
  ships: smallest row value ≥ `x * margin`. (A `pickFloor` sibling is
  trivial and reserved.)
- **`visibleWhen` / `condition`** — boolean-valued expressions (non-zero =
  true) that hide inputs / omit outputs. This is what makes the DC/AC split
  in the MCB example possible without any app code.
- **String concatenation** (`+` with a string operand) for note outputs.

Anything outside the whitelist fails validation at import. There is no file
IO, no reflection, no loops, no user-defined functions — the DSL's
expressive ceiling is intentionally "spreadsheet cell".

---

## 3. Where plugins live in the app

- **Tools home gains an "Extensions" section** under the built-in
  `ToolDestination` cards — installed plugins render as the same tonal
  `ToolCard`s (icon tile + title + subtitle + chevron) with a small
  "extension" badge, so the visual language is unchanged.
- Tapping a plugin card opens **PluginScreen** — the same input → result
  layout the built-in tools use, but rendered *dynamically* from the manifest
  (enum → `FilterChip` row, number → the shared `ToolNumberField`, outputs →
  `ResultLine` / a big display line for `kind: "primary"`).
- The **overflow menu on a plugin screen** gets "Remove extension" (with a
  confirm dialog; mirrors the history clear-all confirm pattern).
- New navigation state: `activePlugin: String?` (plugin id) sits beside
  `activeTool: ToolDestination?` in `AppRoot`; `BackHandler` learns one more
  level. `PluginTags` joins `ToolTags`/`CalcTags` for tests.

---

## 4. Runtime architecture

```
core-plugins/                     NEW pure-JVM module (mirrors core-math rules:
  src/main/kotlin/.../plugins/    zero Android imports, same check-task guard)
    PluginManifest.kt             manifest model + parser
    PluginValidator.kt            schema + caps + expression whitelisting
    PluginEngine.kt               bind inputs -> eval outputs (ordered, conditional)
    PluginExpression.kt           ternary/if/pickCeiling glue over core-math
    Catalog.kt                    bundled plugin_catalog.json model + parser
  src/test/kotlin/...             parser/validator/engine/table/MCB goldens

app/
  data/PluginRepository.kt        Room entity + DAO (id PK, name, version,
                                  enabled, installedAt, fileName, specVersion)
  filesDir/plugins/<id>.json      the imported manifest itself (source of truth
                                  on disk; Room is the index)
  ui/plugins/
    PluginScreen.kt               dynamic input/output rendering
    PluginStoreScreen.kt          offline catalog list (6)
    PluginImport.kt               SAF import + confirm dialog + validation UX
```

### 4.1 The one core-math change: variable bindings

`evaluate(expression, mode)` grows an overload
`evaluate(expression, mode, bindings: Map<String, BigDecimal>)`. The lexer's
identifier branch — which today only knows the constants — resolves unknown
identifiers against the binding map (unknown identifier without a binding =
parse failure, so plugin typos fail loudly, never silently).

This change is for the calculator too: it is the seam later phases use for
named constants. It ships with its own lexer/parser/evaluator tests and does
not alter any existing behavior when the map is empty.

### 4.2 Evaluation pipeline (PluginEngine)

1. Resolve `visibleWhen` per input → collect bound inputs (missing required
   input → "waiting for input" state, like the tools' "—" placeholder).
2. Evaluate outputs **in declared order**; each output's bindings see inputs
   and *already-computed* outputs (declared order = dependency order — the
   validator rejects an output referencing a later output).
3. `condition` on an output hides it; `kind: "primary"` renders big.
4. Format via `formatToolNumber`/`formatFixed` (F14's unified formatters —
   the plugin system is the first consumer that proves F14 mattered).

Deterministic, allocation-light, bounded: one pass of BigDecimal evaluations.

### 4.3 Room registry

`PluginEntity(id, name, version, specVersion, enabled, installedAt, fileName)`
+ `PluginDao` (observeAll, upsert, delete). Installed list is a `Flow` — the
tools home re-renders when plugins come and go. `enabled=false` hides the
card without uninstalling (a future settings page).

### 4.4 Import flow (zero permissions, SAF)

- **Path A — file association.** The manifest registers an intent-filter for
  opening documents (`ACTION_VIEW`, `categoryOpenable`, mime `*/*` +
  `pathPattern` for `.toadext` — imperfect, but the browser's "Open" usually
  offers the app for a downloaded file). Launch → import dialog directly.
- **Path B — in-app picker.** "Import from file" button (store screen and
  overflow) → `ACTION_OPEN_DOCUMENT` (SAF needs **no storage permission**) →
  copy bytes to a temp file → validate.
- Both paths converge on the **confirm dialog**: name, version, author,
  description, input/output summary, checksum when known, and for
  non-catalog plugins an explicit "Not in the toad catalog — install
  anyway?" warning. Install = validate → persist JSON to `filesDir/plugins/`
  → upsert Room row → snackbar.
- Validation failures show the validator's specific message ("input `pf`:
  default 0.95 outside min/max"; "output `mcbRating` references unknown
  identifier `standartRatings`") — typos surface at import, not at use.

### 4.5 Icons without a size-tax

Plugins can't ship drawables. Icons are chosen from a **bundled registry**
(`icon: "ic_tool_calculator"` etc. — the existing tool vectors plus generic
glyphs). Unknown name → neutral default. Zero APK growth from plugins; zero
R8 surprises.

---

## 5. Security & trust model

1. **No code execution.** The runtime is a formula evaluator over a
   whitelisted grammar; there is no API surface a plugin could abuse.
2. **Caps at import** (§2) make resource exhaustion impossible: bounded
   manifest size, bounded expression count/length, no loops → bounded eval.
3. **First-party chain of trust**: the bundled catalog pins sha256 for each
   first-party release asset; importing a file whose hash matches shows
   "toad catalog · verified". A mismatch or unknown file gets the explicit
   unknown-source warning. (Checksums are user signal in v1 — the runtime
   containment is the actual boundary.)
4. **No new permissions, ever.** The plugin system runs entirely inside the
   app sandbox (`filesDir` + Room). The CI `permissions-audit` job continues
   to pass unchanged — that is a *test* of this design, not just a gate.
5. **Version discipline**: `specVersion` floor, `minAppVersion` floor,
   explicit downgrade warning on re-import.

---

## 6. The catalog: scrolling available plugins, offline

The in-app **Plugin store** screen lists the **bundled catalog asset**
(`app/src/main/assets/plugin_catalog.json`), regenerated at build time from
the org's plugin repos (§8). Each entry:

```jsonc
{ "id": "power-mcb-advisor", "name": "MCB / breaker advisor",
  "version": "1.0.0", "author": "dukeoftoad",
  "summary": "MCB rating from AC/DC load inputs", "icon": "ic_tool_calculator",
  "downloadUrl": "https://github.com/dukeoftoad/toadcalc-plugin-mcb/releases/download/v1.0.0/power-mcb-advisor-1.0.0.toadext",
  "sha256": "...", "sizeBytes": 4200, "minAppVersion": "1.3.0" }
```

- Fully offline scroll/search — it ships in the APK.
- **Install button → `ACTION_VIEW` on `downloadUrl`** → the browser
  downloads the `.toadext` → user taps it (Path A) or returns and uses
  "Import from file" (Path B). The app never touches the network; the
  catalog says what exists and where, the browser fetches, the app installs.
- Already-installed entries read "Installed" / "Update" (version compare
  against the registry).
- **Catalog freshness without app releases**: the catalog JSON itself is a
  release asset in the org's `toadPLUGINS` repo; the store's "Refresh
  catalog" button opens that URL in the browser, and importing a
  `.toadcatalog` file replaces the bundled list (same validator path,
  `specVersion` again). New plugins can appear in the store before the next
  app release.

### 6.1 The trade-off the owner should see explicitly

| Option | UX | Promise | Verdict |
|---|---|---|---|
| **A. Bundled catalog + browser-mediated download (this map)** | In-app scroll; install = one browser hop | **Zero permissions intact**, audit untouched | **Recommended** |
| B. Companion app `toadPLUGINS` (network permission lives there; pushes installs via intent) | Smoothest install | Main app stays clean; one more app to maintain | Viable later if A's hop feels heavy |
| C. `INTERNET` permission in the main app | Smoothest | **Breaks the CI gate, the README promise, the About-sheet copy** | Rejected by default — only the owner can retire that promise |

---

## 7. Worked example: the owner's MCB advisor

The §2.1 manifest **is** the first plugin. Behavior with real inputs:

- AC 1φ, 230 V, 2000 W, pf 0.95 → I = 2000/(230·0.95) ≈ **9.16 A** →
  `pickCeiling(9.16, table, 1.25)` → ≥ 11.45 → **16 A MCB**, curve per load
  kind.
- AC 3φ, 400 V, 5500 W, pf 0.9 → I = 5500/(400·1.732·0.9) ≈ **8.82 A** →
  ≥ 11.03 → **16 A**, curve C if motor.
- DC, 12 V, current 10 A → I = **10 A** → ≥ 12.5 → **16 A**.
- Direct current entry instead of power (the DC branch exposes `current`,
  hides `power`/`pf` via `visibleWhen`) — exactly the DC/AC input-switching
  the owner described.

Golden tests for these cases ship in `core-plugins` (pure JVM) — the
plugin's math is CI-pinned before any release.

Sibling first-party plugins that reuse the same runtime (each is a manifest,
not an app release):

| Plugin | Scope |
|---|---|
| `cable-sizing` | Ampacity table + correction factors + voltage-drop check (3%/5%) — natural companion to the MCB advisor |
| `solar-offgrid` | Array → battery bank (autonomy days, depth-of-discharge), inverter headroom, PWM/MPPT current |
| `battery-runtime` | Ah, load W, inverter efficiency → runtime hours |
| `resistor-color` | 4/5-band color → value; SMD codes |
| `emi-loan` | EMI, total interest, simple amortization summary |
| `power-triangle` | P/Q/S/pf/φ solve-given-two |

---

## 8. Publishing model (GitHub, matching the family's release discipline)

- One repo per plugin under `dukeoftoad` (`toadcalc-plugin-mcb`, …). Each
  repo holds the manifest + a test harness (the same pure-JVM engine on CI)
  and attaches the `.toadext` to its releases — exactly like APKs attach to
  app releases today.
- `toadPLUGINS` repo: the **catalog source** + the regenerator script that
  walks plugin repos' latest releases, hashes assets, and emits
  `plugin_catalog.json` (+ a `.toadcatalog` refresh asset). The app repo
  fetches the catalog only at release-bundling time, by CI — never by the
  app at runtime.
- Review bar for first-party: manifest passes the validator, goldens green,
  description honest, no identifier collisions, hash pinned in catalog.

## 9. Testing & CI map

- `core-plugins` (pure JVM): manifest parse/validate negatives (bad caps,
  unknown identifiers, forward output references, spec version ahead of the
  app), engine goldens (the MCB cases above), `pickCeiling` edges,
  conditional visibility.
- App (Robolectric): store list renders catalog entries; install button
  fires the `ACTION_VIEW` intent (Robolectric intent assert); import confirm
  dialog shows manifest metadata; plugin screen computes MCB case 1 via
  typed inputs (`PluginTags`); remove-extension confirm dialog; tools home
  shows the Extensions section with an installed plugin seeded into a fake
  repo.
- CI: the existing `build` job runs `:core-plugins:test` (add it to the
  unit-test step); the size gate watches the +~150–250 KB cost of the JSON
  parser + plugin UI; `permissions-audit` must stay at **zero permissions**
  — it is now also a regression test for the plugin design.

## 10. Rollout plan

| Phase | Content | Gate |
|---|---|---|
| **10.2 — plugin foundation** | `core-plugins` module (manifest/validator/engine/tables/ternary), core-math bindings overload + tests, Room registry, SAF import + confirm dialog, dynamic PluginScreen, tools-home Extensions section, overflow "Remove extension". Ship the MCB manifest bundled for dogfooding via import. | CI green; size delta noted |
| **10.3 — store + catalog** | `plugin_catalog.json` asset, PluginStoreScreen (offline scroll/search), `ACTION_VIEW` install hand-off, `.toadext` intent-filter, catalog refresh import, `toadPLUGINS` repo + regenerator script, `docs/PLUGIN_SPEC.md` (normative author spec) | CI green; audit zero-permission |
| **10.4 — hardening + suite** | First-party plugin suite (§7 table), enable/disable settings page, catalog hash-pinning UX, lint job promoted to blocking (F21 follow-up), optional zip container if assets are ever needed | size + audit |

Serialization choice for 10.2: `kotlinx-serialization-json` (small, clean R8
rules) — alternative is a hand-rolled `org.json` parser if the size gate
bites; the decision gets recorded in the phase WORKLOG either way.

## 11. Risks & mitigations

- **Size gate**: serialization lib + plugin UI ≈ +250 KB against ~2.3 MiB
  headroom at 1.69 MiB current — comfortable, but each phase reports the
  delta in WORKLOG (established discipline).
- **DSL creep**: every new DSL keyword re-opens the security analysis. Rule:
  a new keyword ships only with (a) validator support, (b) tests, (c) a
  WORKLOG paragraph. The spreadsheet ceiling is a feature, not a limit.
- **Import UX friction** (browser hop of Option A): mitigated by the
  `.toadext` file association (tap the download → app opens) and the in-app
  "Import from file" fallback; if the owner finds it heavy, Option B
  (companion app) is the upgrade path that never touches the core promise.
- **Stale catalogs**: catalog refresh import (§6) keeps the store current
  between app releases.
- **Bounded-but-annoying failures**: all manifest errors are import-time
  validation errors with specific messages; the engine itself cannot throw
  at use time beyond math domain errors, which render like the calculator's.

## 12. Explicitly left out (owner decisions, not architecture gaps)

- In-app network fetching (Option C) — needs the owner to consciously retire
  the zero-permission promise. Everything above is built so that decision
  could be made later without rework (the store's data source is one
  interface).
- Plugin-defined scripts/logic (DEX/Lua/JS) — rejected in §1; revisit only
  if a plugin need genuinely cannot be expressed as data (none seen so far).
- Marketplace accounts/payments — out of scope for a personal,
  GPL-3.0-or-later, offline-first family.
