// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2025 toadCALCULATOR contributors
//
// Evaluator on Decimal with a DECIMAL64-like context (16 significant digits,
// banker's rounding) — `0.1 + 0.2` displays as `0.3`, never `0.30000000000000004`.
// Only transcendental functions pass through double; everything else stays decimal.
// Overflow bound: |value| > 1e6144 → Overflow (mirrors the Android core-math bound).

import Decimal from "decimal.js";
import { parse, type AstNode } from "./parser";
import {
  domainError,
  mathError,
  type AngleMode,
  type MathError,
  type MathResult,
} from "./types";

// ---- decimal context (DECIMAL64 analogue) --------------------------------
Decimal.set({
  precision: 16,
  rounding: Decimal.ROUND_HALF_EVEN,
  toExpNeg: -9e15,
  toExpPos: 9e15,
  maxE: 6144,
  minE: -6143,
});

const PI = Decimal.acos(-1);
const E = Decimal.exp(1);

const MAX_FACTORIAL = 1000;

class MathFailure extends Error {
  readonly error: MathError;
  constructor(error: MathError) {
    super(error.message);
    this.error = error;
  }
}

function fail(error: MathError): never {
  throw new MathFailure(error);
}

/** Map a thrown DecimalError onto the typed MathError taxonomy. */
function mapDecimalError(e: unknown): MathError {
  const msg = e instanceof Error ? e.message : String(e);
  if (/division by zero/i.test(msg)) {
    return mathError("DivisionByZero", "Division by zero");
  }
  if (/overflow|infinity/i.test(msg)) {
    return mathError("Overflow", "Result too large");
  }
  if (/nan|invalid/i.test(msg)) {
    return domainError("Invalid operation");
  }
  return mathError("Overflow", "Result too large");
}

/** Run a decimal operation, catching library errors into typed failures. */
function dec(fn: () => Decimal): Decimal {
  try {
    const value = fn();
    if (!value.isFinite()) {
      if (value.isNaN()) return fail(domainError("Invalid operation"));
      return fail(mathError("Overflow", "Result too large"));
    }
    return value;
  } catch (e) {
    if (e instanceof MathFailure) throw e;
    throw new MathFailure(mapDecimalError(e));
  }
}

// ---- trigonometry (double path + exact snapping) ---------------------------

function degNorm(x: number, full: number): number {
  const r = x % full;
  return r < 0 ? r + full : r;
}

function sinFn(x: Decimal, mode: AngleMode): Decimal {
  if (mode === "deg") {
    const d = x.toNumber();
    const r = degNorm(d, 360);
    if (r === 0 || r === 180) return new Decimal(0);
    if (r === 90) return new Decimal(1);
    if (r === 270) return new Decimal(-1);
  }
  const v = Math.sin(mode === "deg" ? (x.toNumber() * Math.PI) / 180 : x.toNumber());
  if (!Number.isFinite(v)) fail(mathError("Overflow", "Result too large"));
  return new Decimal(Math.abs(v) < 1e-12 ? 0 : v);
}

function cosFn(x: Decimal, mode: AngleMode): Decimal {
  if (mode === "deg") {
    const d = x.toNumber();
    const r = degNorm(d, 360);
    if (r === 90 || r === 270) return new Decimal(0);
    if (r === 0) return new Decimal(1);
    if (r === 180) return new Decimal(-1);
  }
  const v = Math.cos(mode === "deg" ? (x.toNumber() * Math.PI) / 180 : x.toNumber());
  if (!Number.isFinite(v)) fail(mathError("Overflow", "Result too large"));
  return new Decimal(Math.abs(v) < 1e-12 ? 0 : v);
}

function tanFn(x: Decimal, mode: AngleMode): Decimal {
  if (mode === "deg") {
    const d = x.toNumber();
    const r = degNorm(d, 180);
    if (r === 0) return new Decimal(0);
    if (r === 90) {
      fail(domainError("tan(90°) is undefined"));
    }
  }
  const v = Math.tan(mode === "deg" ? (x.toNumber() * Math.PI) / 180 : x.toNumber());
  if (!Number.isFinite(v)) fail(mathError("Overflow", "Result too large"));
  return new Decimal(Math.abs(v) < 1e-12 ? 0 : v);
}

function asinFn(x: Decimal, mode: AngleMode): Decimal {
  const n = x.toNumber();
  if (n < -1 || n > 1 || Number.isNaN(n)) {
    fail(domainError("asin domain is [-1, 1]"));
  }
  const v = Math.asin(n);
  return toAngleOut(v, mode);
}

function acosFn(x: Decimal, mode: AngleMode): Decimal {
  const n = x.toNumber();
  if (n < -1 || n > 1 || Number.isNaN(n)) {
    fail(domainError("acos domain is [-1, 1]"));
  }
  const v = Math.acos(n);
  return toAngleOut(v, mode);
}

function atanFn(x: Decimal, mode: AngleMode): Decimal {
  return toAngleOut(Math.atan(x.toNumber()), mode);
}

function toAngleOut(radians: number, mode: AngleMode): Decimal {
  const v = mode === "deg" ? (radians * 180) / Math.PI : radians;
  if (!Number.isFinite(v)) fail(mathError("Overflow", "Result too large"));
  return new Decimal(Math.abs(v) < 1e-12 ? 0 : v);
}

// ---- hyperbolics / misc ----------------------------------------------------

function sinhFn(x: Decimal): Decimal {
  const v = Math.sinh(x.toNumber());
  if (!Number.isFinite(v)) fail(mathError("Overflow", "sinh overflow"));
  return new Decimal(v);
}
function coshFn(x: Decimal): Decimal {
  const v = Math.cosh(x.toNumber());
  if (!Number.isFinite(v)) fail(mathError("Overflow", "cosh overflow"));
  return new Decimal(v);
}
function tanhFn(x: Decimal): Decimal {
  const v = Math.tanh(x.toNumber());
  if (!Number.isFinite(v)) fail(mathError("Overflow", "tanh overflow"));
  return new Decimal(v);
}

function factorial(n: Decimal): Decimal {
  if (!n.isInteger()) fail(domainError("Factorial needs an integer"));
  if (n.isNegative()) fail(domainError("Factorial of a negative number"));
  if (n.gt(MAX_FACTORIAL)) fail(mathError("Overflow", "Factorial too large"));
  let acc = new Decimal(1);
  const steps = n.toNumber();
  for (let i = 2; i <= steps; i++) acc = dec(() => acc.mul(i));
  return acc;
}

function power(a: Decimal, b: Decimal): Decimal {
  if (a.isZero()) {
    if (b.isZero()) fail(mathError("Undefined", "0^0 is undefined"));
    if (b.isNegative()) fail(mathError("DivisionByZero", "Division by zero"));
  }
  if (b.isInteger()) {
    if (b.abs().gt(1e6)) fail(mathError("Overflow", "Exponent too large"));
    return dec(() => a.pow(b));
  }
  if (a.isNegative()) {
    fail(domainError("Negative base with non-integer exponent"));
  }
  return dec(() => a.pow(b));
}

// ---- function table (19 functions; DEG/RAD affects trig only) --------------

type FnImpl = (x: Decimal, mode: AngleMode) => Decimal;

export const FUNCTIONS: Record<string, FnImpl> = {
  sin: (x, m) => sinFn(x, m),
  cos: (x, m) => cosFn(x, m),
  tan: (x, m) => tanFn(x, m),
  asin: (x, m) => asinFn(x, m),
  acos: (x, m) => acosFn(x, m),
  atan: (x, m) => atanFn(x, m),
  sinh: (x) => sinhFn(x),
  cosh: (x) => coshFn(x),
  tanh: (x) => tanhFn(x),
  ln: (x) => {
    if (x.isNegative()) fail(domainError("ln domain is (0, ∞)"));
    if (x.isZero()) fail(domainError("ln(0) is undefined"));
    return dec(() => x.ln());
  },
  log: (x) => {
    if (x.isNegative()) fail(domainError("log domain is (0, ∞)"));
    if (x.isZero()) fail(domainError("log(0) is undefined"));
    return dec(() => x.log());
  },
  log2: (x) => {
    if (x.isNegative()) fail(domainError("log2 domain is (0, ∞)"));
    if (x.isZero()) fail(domainError("log2(0) is undefined"));
    return dec(() => x.log(2));
  },
  sqrt: (x) => {
    if (x.isNegative()) fail(domainError("sqrt of a negative number"));
    return dec(() => x.sqrt());
  },
  cbrt: (x) => {
    if (x.isNegative()) return dec(() => x.neg().cbrt().neg());
    return dec(() => x.cbrt());
  },
  abs: (x) => dec(() => x.abs()),
  exp: (x) => dec(() => x.exp()),
  floor: (x) => dec(() => x.floor()),
  ceil: (x) => dec(() => x.ceil()),
  round: (x) => dec(() => x.toDecimalPlaces(0, Decimal.ROUND_HALF_UP)),
};

// ---- tree-walking evaluator -------------------------------------------------

function evalNode(node: AstNode, mode: AngleMode): Decimal {
  switch (node.kind) {
    case "number":
      return dec(() => new Decimal(node.value));

    case "constant":
      return node.name === "pi" ? PI : E;

    case "group":
      // parentheses isolate: a % inside evaluates plain (no percent-of-total)
      return evalNode(node.inner, mode);

    case "unary": {
      const v = evalNode(node.operand, mode);
      return node.op === "-" ? dec(() => v.neg()) : v;
    }

    case "factorial":
      return factorial(evalNode(node.operand, mode));

    case "percent":
      // standalone / non-relative context: x% = x/100
      return dec(() => evalNode(node.operand, mode).div(100));

    case "binary": {
      const left = evalNode(node.left, mode);
      const right = node.right;

      // percent-of-total: only when the right operand is a DIRECT percent node
      // of a `+`/`-` (a Group wrapper deliberately breaks this, so `200+(10%)`
      // is plain addition while `200+10%` is 220).
      if (node.op === "+" || node.op === "-") {
        if (right.kind === "percent") {
          const b = dec(() => evalNode(right.operand, mode).div(100));
          const share = dec(() => left.mul(b));
          return node.op === "+" ? dec(() => left.plus(share)) : dec(() => left.minus(share));
        }
        const r = evalNode(right, mode);
        return node.op === "+" ? dec(() => left.plus(r)) : dec(() => left.minus(r));
      }

      if (node.op === "*" || node.op === "/") {
        // A × B% = A × B/100 — and the same plain /100 for division
        const r =
          right.kind === "percent"
            ? dec(() => evalNode(right.operand, mode).div(100))
            : evalNode(right, mode);
        if (node.op === "*") return dec(() => left.mul(r));
        if (r.isZero()) fail(mathError("DivisionByZero", "Division by zero"));
        return dec(() => left.div(r));
      }

      if (node.op === "mod") {
        const r = evalNode(right, mode);
        if (r.isZero()) fail(mathError("DivisionByZero", "Modulo by zero"));
        return dec(() => left.mod(r));
      }

      // "^"
      return power(left, evalNode(right, mode));
    }

    case "call": {
      const fn = FUNCTIONS[node.name];
      if (!fn) fail(domainError(`Unknown function ${node.name}`));
      return fn(evalNode(node.arg, mode), mode);
    }
  }
}

/**
 * The single engine entry point.
 * `evaluate(input, angleMode) → MathResult` — Success(Decimal) | Failure(MathError).
 * Never throws for foreseeable math conditions.
 */
export function evaluate(input: string, angleMode: AngleMode): MathResult {
  if (!input.trim()) {
    return { ok: false, error: { type: "SyntaxError", message: "Empty expression" } };
  }
  try {
    const { ast, openParens } = parse(input);
    if (openParens > 0) {
      return {
        ok: false,
        error: { type: "UnbalancedParentheses", message: "Missing closing parenthesis" },
      };
    }
    const value = evalNode(ast, angleMode);
    if (!value.isFinite()) {
      return { ok: false, error: { type: "Overflow", message: "Result too large" } };
    }
    return { ok: true, value };
  } catch (e) {
    if (e instanceof MathFailure) {
      return { ok: false, error: e.error };
    }
    if (e instanceof Object && "error" in e && e.error && typeof e.error === "object") {
      // ParseFailure / LexFailure carry a typed MathError
      return { ok: false, error: e.error as MathError };
    }
    return { ok: false, error: mapDecimalError(e) };
  }
}
