// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2025 toadCALCULATOR contributors
//
// NumberFormatter: configurable significant digits (6/8/10/12/15, default 12),
// trailing-zero stripping, scientific notation for |x| ≥ 1e12 or 0 < |x| < 1e-9,
// and thousands grouping applied to the DISPLAY STRING only (never re-parsed).

import Decimal from "decimal.js";

export interface FormatOptions {
  /** significant digits: 6 | 8 | 10 | 12 | 15 (default 12) */
  precision: number;
  /** insert thousands separators in the integer part (display only) */
  groupThousands: boolean;
}

const SUPERSCRIPT: Record<string, string> = {
  "0": "⁰",
  "1": "¹",
  "2": "²",
  "3": "³",
  "4": "⁴",
  "5": "⁵",
  "6": "⁶",
  "7": "⁷",
  "8": "⁸",
  "9": "⁹",
  "-": "⁻",
};

function superscript(n: number): string {
  return String(n)
    .split("")
    .map((c) => SUPERSCRIPT[c] ?? c)
    .join("");
}

function groupInteger(digits: string): string {
  return digits.replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

/**
 * Format a Decimal for display.
 * - rounds to `precision` significant digits (half-even, like DECIMAL64)
 * - strips trailing zeros: sin(180°) renders as exactly `0`
 * - |x| ≥ 1e12 or 0 < |x| < 1e-9 → scientific notation `1.5×10¹²`
 * - thousands separators are cosmetic and never re-parsed by the engine
 */
export function formatNumber(x: Decimal, opts: FormatOptions): string {
  const precision = Math.min(15, Math.max(6, Math.round(opts.precision)));

  if (!x.isFinite()) return "Error";
  if (x.isZero()) return "0";

  const sd = x.toSignificantDigits(precision, Decimal.ROUND_HALF_EVEN);
  if (sd.isZero()) return "0";

  const negative = sd.isNegative();
  const abs = sd.abs();
  const e = abs.e; // power of ten of the leading digit

  // scientific thresholds: ≥ 1e12, or < 1e-9 (i.e. exponent ≤ -10)
  if (e >= 12 || e <= -10) {
    // coefficient digits straight from the decimal (no plain-string round trip)
    const chunks = abs.d ?? abs.c ?? [0];
    let digits = chunks.join("");
    digits = digits.replace(/0+$/, "") || "0";
    const mantissa = digits.length > 1 ? `${digits[0]}.${digits.slice(1)}` : digits[0];
    return `${negative ? "-" : ""}${mantissa}×10${superscript(e)}`;
  }

  // plain notation
  let plain = abs.toFixed(Math.max(0, abs.dp()));
  if (plain.includes(".")) {
    plain = plain.replace(/0+$/, "").replace(/\.$/, "");
  }
  if (opts.groupThousands) {
    const [intPart, fracPart] = plain.split(".");
    plain = fracPart !== undefined
      ? `${groupInteger(intPart)}.${fracPart}`
      : groupInteger(intPart);
  }
  return `${negative ? "-" : ""}${plain}`;
}

/** Same value as a parseable plain string (for ans-chaining after `=`). */
export function toPlainString(x: Decimal, precision: number): string {
  const sd = x.toSignificantDigits(
    Math.min(15, Math.max(6, Math.round(precision))),
    Decimal.ROUND_HALF_EVEN,
  );
  if (sd.isZero()) return "0";
  return sd.toString();
}
