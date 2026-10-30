# toadCALCULATOR

A personal calculator that respects you: **offline-first, no ads, no tracking, no special permissions.**

![Platform](https://img.shields.io/badge/platform-web%20%7C%20android-2f9e6e)
![License](https://img.shields.io/badge/license-GPL--3.0--or--later-2f9e6e)

## Features

- **Precision math engine** — decimal arithmetic on a DECIMAL64-analogue context
  (16 significant digits, banker's rounding): `0.1 + 0.2` is exactly `0.3`.
  Only transcendental functions pass through double, then snap (`sin(180°) = 0`).
- **Full operator semantics** — `+ − × ÷`, right-associative `^` binding tighter
  than unary minus (`-2^2 = -4`), postfix `!`/`%`, implicit multiplication
  (`2π`, `3sin(30)`), binary modulo vs postfix percent resolved at lex time
  (`10 % 3 = 1`, `5 % -3` parses as `5% − 3`).
- **Spec-mandated percent-of-total** — `A + B% = A + A·B/100` (`200+10% = 220`),
  `A × B% = A × B/100`, standalone `x% = x/100`, and `(10%)` evaluates plain.
- **Scientific mode** — sin cos tan asin acos atan sinh cosh tanh ln log log2
  sqrt cbrt abs exp floor ceil round, `π`, `e`, factorial; a 2ND toggle reveals
  inverse/secondary functions; DEG/RAD chip with exact snapping.
- **NumberFormatter** — configurable significant digits (6/8/10/12/15),
  trailing-zero stripping, scientific notation for `|x| ≥ 1e12` or
  `0 < |x| < 1e-9`, thousands separators display-only.
- **Typed errors, never crashes** — DivisionByZero, DomainError, SyntaxError
  (with position), Overflow, Undefined, UnbalancedParentheses render inline.
- **History** — persisted locally, newest first, tap-to-reload, per-row delete,
  clear-all with confirmation; failed expressions are never saved.
- **Settings** — theme (system/light/dark), precision, default angle mode,
  thousands separator, haptics — all persisted on-device.
- **Offline tools** — unit converter (8 categories, exact international
  factors, offset-aware temperature), BMI (WHO classification + healthy
  range), age calculator (Y/M/D breakdown, next-birthday countdown),
  date difference (inclusive/exclusive, calendar-aware).
- **Desktop keyboard support** — digits, operators, `Enter`, `Backspace`,
  `Escape`; long-press ⌫ to clear all.

## Architecture (web edition)

```
src/
├── lib/
│   ├── math/            # pure TS engine, zero framework deps
│   │   ├── types.ts     #   AngleMode, MathResult = Success|Failure
│   │   ├── lexer.ts     #   tokens, positions, % classification, e-notation
│   │   ├── parser.ts    #   recursive descent → AST (Group-wrapped parens)
│   │   ├── evaluator.ts #   decimal.js @ 16 digits, 1e6144 overflow bound
│   │   ├── formatter.ts #   significant digits, ×10ⁿ notation, grouping
│   │   └── index.ts     #   evaluate(input, angleMode) → MathResult
│   ├── calc/editing.ts  # token-aware backspace + insertion rules
│   └── tools/           # units (exact factors), bmi, datetime (UTC-safe)
├── store/calculator.ts  # single zustand store, persisted to localStorage
├── hooks/               # hydration guard, long-press, today-iso
├── components/
│   ├── calculator/      # display, keypad (basic+sci), history sheet, settings
│   └── tools/           # converter, BMI, age, date-diff
└── app/                 # single route (/), theme provider, sticky footer
```

Single-entry engine: `evaluate(input: string, angleMode: AngleMode): MathResult`.

The engine is a line-for-line semantic mirror of the Android `:core-math`
module (BigDecimal/DECIMAL64) in the private repository
[`dukeoftoad/toadCALCULATOR`](https://github.com/dukeoftoad/toadCALCULATOR),
which documents the original phase-by-phase build in `WORKLOG.md`.

## Build & run

```bash
bun install
bun run dev        # development on :3000
bun run lint       # eslint (clean)
```

No environment variables, no database, no network access required — by design.

## License

Copyright © 2025 toadCALCULATOR contributors.

This program is free software: you can redistribute it and/or modify it under
the terms of the **GNU General Public License** as published by the Free
Software Foundation, either **version 3** of the License, or (at your option)
any later version (GPL-3.0-or-later). There is NO WARRANTY, to the extent
permitted by law. See [LICENSE](./LICENSE) for the full text.

Every source file carries an `SPDX-License-Identifier: GPL-3.0-or-later` header.
