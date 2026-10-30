// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2025 toadCALCULATOR contributors
//
// Hand-written lexer. Mirrors the Android core-math Lexer decisions:
// - token positions are reported for SyntaxError
// - e-notation vs Euler's constant disambiguation (2e3 = 2000, 2e = 2·e)
// - unicode operator mapping (× ÷ − –)
// - log2 as a single token
// - % classified AT LEX TIME: binary modulo when an operand follows,
//   otherwise postfix percent (`5 % -3` therefore parses as `5% − 3`).

import { syntaxError, type MathError } from "./types";

export type TokenKind =
  | "number"
  | "ident" // constant or function name (value normalized)
  | "op" // + - * / ^ !
  | "mod" // binary modulo (operand follows)
  | "percent" // postfix percent (no operand follows)
  | "lparen"
  | "rparen"
  | "end";

export interface Token {
  kind: TokenKind;
  /** normalized value: number text, ident name, or operator char */
  value: string;
  pos: number;
}

/** Constants recognized as identifiers. */
export const CONSTANTS = new Set(["pi", "e"]);

/** Function names recognized by the lexer (longest-first matching). */
export const FUNCTIONS = [
  "asin",
  "acos",
  "atan",
  "sinh",
  "cosh",
  "tanh",
  "log2",
  "sqrt",
  "cbrt",
  "abs",
  "exp",
  "floor",
  "ceil",
  "round",
  "sin",
  "cos",
  "tan",
  "ln",
  "log",
].sort((a, b) => b.length - a.length);

const OPERATORS = new Set(["+", "-", "*", "/", "^", "!"]);

function isDigit(c: string): boolean {
  return c >= "0" && c <= "9";
}

function isLetter(c: string): boolean {
  return (c >= "a" && c <= "z") || (c >= "A" && c <= "Z");
}

function isOperandStart(c: string): boolean {
  return isDigit(c) || c === "." || c === "(" || isLetter(c) || c === "π";
}

/** Normalize unicode input characters to canonical operators. */
function normalizeChar(c: string): string {
  switch (c) {
    case "×":
    case "·":
      return "*";
    case "÷":
    case ":":
      return "/";
    case "−":
    case "–":
    case "—":
      return "-";
    case "π":
      return "π"; // kept, mapped to ident below
    default:
      return c;
  }
}

export class LexFailure extends Error {
  readonly error: MathError;
  constructor(error: MathError) {
    super(error.message);
    this.error = error;
  }
}

/**
 * Tokenize `input`. Throws LexFailure (wrapping a MathError) on invalid input.
 */
export function tokenize(input: string): Token[] {
  const tokens: Token[] = [];
  let i = 0;
  const n = input.length;

  while (i < n) {
    const raw = input[i];
    const c = normalizeChar(raw);

    if (c === " " || c === "\t" || c === "\n" || c === "\r") {
      i++;
      continue;
    }

    // π constant
    if (c === "π") {
      tokens.push({ kind: "ident", value: "pi", pos: i });
      i++;
      continue;
    }

    // numbers: 123 | 123.45 | .5 — with e-notation tail disambiguation
    if (isDigit(c) || (c === "." && i + 1 < n && isDigit(input[i + 1]))) {
      const start = i;
      while (i < n && isDigit(input[i])) i++;
      if (i < n && input[i] === ".") {
        i++;
        while (i < n && isDigit(input[i])) i++;
      }
      // e-notation: only when `e`/`E` is followed by digits (optionally signed).
      // A bare trailing e (e.g. "2e") stays the Euler constant → implicit mult.
      if (i < n && (input[i] === "e" || input[i] === "E")) {
        let j = i + 1;
        if (j < n && (input[j] === "+" || input[j] === "-")) j++;
        if (j < n && isDigit(input[j])) {
          i = j;
          while (i < n && isDigit(input[i])) i++;
        }
      }
      tokens.push({ kind: "number", value: input.slice(start, i), pos: start });
      continue;
    }

    // identifiers / functions
    if (isLetter(c)) {
      const start = i;
      let j = i;
      while (j < n && isLetter(normalizeChar(input[j]))) j++;
      // `log2` contains a digit — extend the letter run when followed by '2'
      let word = input.slice(start, j).toLowerCase();
      if (word === "log" && j < n && input[j] === "2") {
        j++;
        word = "log2";
      }
      // longest-first function match
      const fn = FUNCTIONS.find((f) => word.startsWith(f));
      if (fn) {
        tokens.push({ kind: "ident", value: fn, pos: start });
        i = start + fn.length;
        continue;
      }
      if (CONSTANTS.has(word)) {
        tokens.push({ kind: "ident", value: word, pos: start });
        i = j;
        continue;
      }
      throw new LexFailure(
        syntaxError(start, `Unknown name "${input.slice(start, j)}"`),
      );
    }

    // modulo vs percent, decided here by lookahead
    if (c === "%") {
      let j = i + 1;
      while (j < n && /\s/.test(input[j])) j++;
      const next = j < n ? normalizeChar(input[j]) : "";
      tokens.push({
        kind: isOperandStart(next) ? "mod" : "percent",
        value: "%",
        pos: i,
      });
      i++;
      continue;
    }

    if (c === "(") {
      tokens.push({ kind: "lparen", value: "(", pos: i });
      i++;
      continue;
    }
    if (c === ")") {
      tokens.push({ kind: "rparen", value: ")", pos: i });
      i++;
      continue;
    }
    if (OPERATORS.has(c)) {
      tokens.push({ kind: "op", value: c, pos: i });
      i++;
      continue;
    }

    throw new LexFailure(
      syntaxError(i, `Unexpected character "${raw}"`),
    );
  }

  tokens.push({ kind: "end", value: "", pos: n });
  return tokens;
}
