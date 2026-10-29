# toadCALCULATOR

[![build](https://github.com/dukeoftoad/toadCALCULATOR/actions/workflows/build.yml/badge.svg)](https://github.com/dukeoftoad/toadCALCULATOR/actions/workflows/build.yml)

A personal, offline-first Android calculator. **Zero permissions, zero ads, zero tracking, zero network access.**

- **Status:** under active development (see [WORKLOG.md](WORKLOG.md) for the phase-by-phase log).

## Features (planned)

- Expression calculator with live preview (`BigDecimal` engine, 12-significant-digit display)
- Scientific mode (trig, hyperbolics, logs, roots, factorial, π/e, DEG/RAD)
- Local history (Room), settings (DataStore)
- Offline tools: unit converter, BMI, age, date difference
- Material 3 UI, light/dark themes, full TalkBack labels
- Release APK under 4 MiB, enforced by CI

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
./gradlew :core-math:test        # pure-JVM engine tests
./gradlew testDebugUnitTest      # all unit tests (app + core-data, incl. Robolectric)
```

## Architecture (text diagram)

```
┌─────────────────────────────── :app (com.toadcalculator) ────────────────────────────────┐
│ MainActivity (single activity)                                                           │
│   └─ ui/   AppRoot → ui/calculator · ui/history · ui/tools · ui/settings · ui/theme      │
│   └─ viewmodel/  StateFlow + unidirectional data flow                                    │
│   └─ AppGraph  (manual constructor injection — no DI framework)                          │
└───────────────┬──────────────────────────────────────────────┬───────────────────────────┘
                │ depends on                                   │ depends on
┌───────────────▼──────────────────────┐   ┌───────────────────▼──────────────────────────┐
│ :core-math (com.toadcalculator.math) │   │ :core-data (com.toadcalculator.data)         │
│ Pure Kotlin, zero Android imports    │   │ Room history · DataStore settings            │
│ Lexer → Parser → AST → Evaluator     │   │ HistoryRepository / SettingsRepository APIs  │
│ NumberFormatter · AngleMode          │   │                                              │
└──────────────────────────────────────┘   └──────────────────────────────────────────────┘
```

`CI (.github/workflows/build.yml)` runs on every push/PR: unit tests for all modules, debug +
release APK builds, a 4 MiB release size gate, and a merged-manifest permission audit printed to
the log.
