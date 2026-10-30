// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2025 toadCALCULATOR contributors
//
// Core types for the toadCALCULATOR math engine.
// Mirrors the semantics of the Android :core-math module (BigDecimal/DECIMAL64,
// typed MathError, percent-of-total semantics, 1e6144 overflow bound).

import type Decimal from "decimal.js";

/** Angle interpretation for trigonometric functions. */
export type AngleMode = "deg" | "rad";

/** Every distinct failure the engine can report. */
export type MathErrorType =
  | "DivisionByZero"
  | "DomainError"
  | "SyntaxError"
  | "Overflow"
  | "Undefined"
  | "UnbalancedParentheses";

/** A typed evaluation failure. `position` is only set for SyntaxError. */
export interface MathError {
  type: MathErrorType;
  message: string;
  position?: number;
}

/**
 * The single engine result type:
 * Success(value) | Failure(error) — never throws for mathematically
 * foreseeable conditions (syntax, domain, overflow, division by zero...).
 */
export type MathResult =
  | { ok: true; value: Decimal }
  | { ok: false; error: MathError };

export function syntaxError(position: number, message: string): MathError {
  return { type: "SyntaxError", message, position };
}

export function domainError(message: string): MathError {
  return { type: "DomainError", message };
}

export function mathError(type: MathErrorType, message: string): MathError {
  return { type, message };
}
