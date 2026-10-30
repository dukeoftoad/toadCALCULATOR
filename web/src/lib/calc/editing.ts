// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2025 toadCALCULATOR contributors
//
// Input editing helpers: token-aware backspace (whole number runs, whole
// function names, whole constants) and smart insertion rules — the web
// mirror of the Android backspaceToken/input token logic.

const TRAILING_PATTERNS: RegExp[] = [
  // whole number run, including a pasted e-notation tail (1.5E-3)
  /(?:\d+\.?\d*(?:[Ee][+-]?\d+)?|\.\d+)$/,
  // whole function name + its opening parenthesis
  /(?:asin|acos|atan|sinh|cosh|tanh|log2|sqrt|cbrt|abs|exp|floor|ceil|round|sin|cos|tan|ln|log)\($/,
  // whole operator / parenthesis / postfix / separator
  /[+\-−–*×·/÷^%!()π,]$/,
];

/**
 * Delete one logical token from the end of `expr`.
 * "sin(30" → "30"; "12.5+3" → "12.5+"; "12.5" → "".
 * Token-aware means numbers vanish as a whole, operators one at a time.
 */
export function backspaceToken(expr: string): string {
  if (expr.length === 0) return "";
  for (const re of TRAILING_PATTERNS) {
    const m = expr.match(re);
    if (m && m[0].length > 0) {
      return expr.slice(0, expr.length - m[0].length);
    }
  }
  return expr.slice(0, -1);
}

const OPERATORS = new Set(["+", "-", "×", "÷", "^"]);

export function endsWithOperator(expr: string): boolean {
  return expr.length > 0 && OPERATORS.has(expr[expr.length - 1]);
}

/** count of '(' minus ')' — used to decide whether ')' may be inserted */
export function openParenBalance(expr: string): number {
  let bal = 0;
  for (const ch of expr) {
    if (ch === "(") bal++;
    else if (ch === ")") bal--;
  }
  return bal;
}

/** the trailing number segment of expr, or "" — used for '.' validation */
export function trailingNumber(expr: string): string {
  const m = expr.match(/(\d+\.?\d*|\.\d+)$/);
  return m ? m[0] : "";
}
