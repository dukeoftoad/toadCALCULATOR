# toadCALCULATOR

[![build](https://github.com/dukeoftoad/toadCALCULATOR/actions/workflows/build.yml/badge.svg)](https://github.com/dukeoftoad/toadCALCULATOR/actions/workflows/build.yml)

A personal, offline-first Android calculator. **Zero permissions, zero ads, zero tracking, zero network access.**

- **Status:** feature-complete through Phase 9.3 — modern toad identity, signed release APKs, offline tools. Phase-by-phase handoff in [WORKLOG.md](WORKLOG.md).

## Features

- **Expression calculator** with live result preview — `BigDecimal` engine (`MathContext.DECIMAL64`), so `0.1+0.2` displays `0.3`
- Operator precedence and associativity: `-2^2 = -4`, right-associative `^`, postfix `!` and `%`, implicit multiplication (`2(3+4)`, `2π`)
- Percent semantics: `200+10% = 220`, `200×10% = 20`, standalone `50% = 0.5`, and `10 % 3 = 1` (modulo)
- 19 functions (`sin cos tan asin acos atan sinh cosh tanh ln log log2 sqrt cbrt abs exp floor ceil round`) in DEG and RAD
- Scientific keypad with a 2nd-functions row (inverse trig, log2, cbrt, abs) and a DEG/RAD indicator
- Display formatting: configurable 6/8/10/12/15 significant digits, trailing-zero stripping, scientific notation outside 1e-9…1e12, optional thousands grouping
- **History** (Room): newest first, tap to reload an expression, swipe-or-button delete, clear-all with confirmation; failed expressions are never stored
- **Settings** (DataStore): theme (system/light/dark), precision, default angle mode, haptics, clear history, About
- **Offline tools:** unit converter (8 categories incl. temperature offset math), BMI with WHO categories, age calculator, date difference with inclusive/exclusive toggle
- Material 3, toad-green palette, light/dark, adaptive icon, themed splash, full TalkBack labels, 48 dp touch targets
- Release APK **under 4 MiB**, enforced by CI; merged manifest audited to contain **zero permissions**

## License

Copyright (C) 2025 toadCALCULATOR contributors

This program is free software: you can redistribute it and/or modify it under the terms of the
GNU General Public License as published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version. See [LICENSE](LICENSE).

Every source file carries an `SPDX-License-Identifier: GPL-3.0-or-later` header.

## Build

Requirements: JDK 21, Android SDK (API 36).

```bash
./gradlew assembleDebug          # debug APK
./gradlew :app:assembleRelease   # R8-minified release APK (unsigned)
./gradlew :core-math:test        # pure-JVM engine + tools tests
./gradlew testDebugUnitTest      # all unit tests (app + core-data, incl. Robolectric Compose tests)
./gradlew :core-math:check       # tests + zero-Android-imports guard
```

## Architecture (text diagram)

```
┌─────────────────────────────── :app (com.toadcalculator) ────────────────────────────────┐
│ MainActivity (single activity, Compose only)                                             │
│   └─ ui/  AppRoot (theme host + state-based navigation)                                  │
│        ├─ ui/calculator  display + live preview + keypad (+ scientific block, 2nd keys)  │
│        ├─ ui/history     Room-backed bottom sheet (load / swipe-delete / clear-all)      │
│        ├─ ui/settings    theme · precision · angle · haptics · clear · about             │
│        ├─ ui/tools       converter · BMI · age · date difference (self-contained)        │
│        └─ ui/theme       Material 3, dynamic color off, toad-green palette               │
│   └─ viewmodel/  StateFlow + unidirectional data flow (Calculator, Settings)             │
│   └─ ToadApplication → AppGraph (manual constructor injection — no DI framework)         │
└───────────────┬──────────────────────────────────────────────┬───────────────────────────┘
                │ depends on                                   │ depends on
┌───────────────▼──────────────────────┐   ┌───────────────────▼──────────────────────────┐
│ :core-math (com.toadcalculator.math) │   │ :core-data (com.toadcalculator.data)         │
│ Pure Kotlin, zero Android imports    │   │ Room history (entity/DAO/db/repository)      │
│ evaluate(input, angleMode): Result   │   │ DataStore settings (theme/precision/angle/   │
│ Lexer → Parser → AST → Evaluator     │   │   haptics) behind testable interfaces        │
│ Functions · NumberFormatter          │   │                                              │
│ tools/ units · BMI · age · date-diff │   │                                              │
└──────────────────────────────────────┘   └──────────────────────────────────────────────┘
```

**Test suite:** 308 JVM/Robolectric tests across the three modules, including a 1000-expression
seeded fuzz test, an adversarial-input test, WHO/unit boundary tables, and Robolectric Compose
tests for every scientific key and every tool.

**CI** (`.github/workflows/build.yml`) runs on every push/PR: unit tests for all modules, debug +
release APK builds, a 4 MiB release size gate, artifact upload (30-day retention), and a
merged-manifest permission audit printed to the log.
