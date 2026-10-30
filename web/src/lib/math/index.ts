// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2025 toadCALCULATOR contributors
//
// Public API of the toadCALCULATOR math engine (pure TypeScript, zero
// framework dependencies — the web mirror of the Android :core-math module).

export type { AngleMode, MathError, MathErrorType, MathResult } from "./types";
export { evaluate, FUNCTIONS as FUNCTION_NAMES } from "./evaluator";
export { formatNumber, toPlainString, type FormatOptions } from "./formatter";
export { CONSTANTS, FUNCTIONS as LEXER_FUNCTIONS } from "./lexer";
export type { Token, TokenKind } from "./lexer";
