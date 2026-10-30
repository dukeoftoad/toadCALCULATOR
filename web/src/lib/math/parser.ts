// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2025 toadCALCULATOR contributors
//
// Recursive-descent parser. Precedence (low → high):
//   additive (+ -) < multiplicative (* / % + implicit ×) < unary (+ -)
//   < power (^, right-assoc, binds tighter than unary minus: -2^2 = -4)
//   < postfix (!, %) < primary (number, π, e, functions, parentheses)
//
// Implicit multiplication sits at multiplicative level: 2π, 3sin(30), (1+2)(3+4).
// Parenthesized expressions are wrapped in a Group node so that a percent
// inside parentheses evaluates plain: `(10%)` = 0.1, while `200+10%` = 220.

import { tokenize, LexFailure, type Token } from "./lexer";
import { syntaxError, type MathError } from "./types";

export type AstNode =
  | { kind: "number"; value: string; pos: number }
  | { kind: "constant"; name: "pi" | "e"; pos: number }
  | { kind: "call"; name: string; arg: AstNode; pos: number }
  | { kind: "group"; inner: AstNode; pos: number }
  | { kind: "binary"; op: "+" | "-" | "*" | "/" | "mod" | "^"; left: AstNode; right: AstNode; pos: number }
  | { kind: "unary"; op: "+" | "-"; operand: AstNode; pos: number }
  | { kind: "factorial"; operand: AstNode; pos: number }
  | { kind: "percent"; operand: AstNode; pos: number };

export class ParseFailure extends Error {
  readonly error: MathError;
  constructor(error: MathError) {
    super(error.message);
    this.error = error;
  }
}

export interface ParseOutput {
  ast: AstNode;
  openParens: number;
}

class Parser {
  private tokens: Token[];
  private idx = 0;
  openParens = 0;

  constructor(input: string) {
    this.tokens = tokenize(input);
  }

  private peek(): Token {
    return this.tokens[this.idx];
  }

  private advance(): Token {
    return this.tokens[this.idx++];
  }

  private fail(pos: number, message: string): never {
    throw new ParseFailure(syntaxError(pos, message));
  }

  /** expression = additive */
  parse(): AstNode {
    const node = this.parseAdditive();
    const t = this.peek();
    if (t.kind !== "end") {
      if (t.kind === "rparen") {
        this.fail(t.pos, "Unexpected ')'");
      }
      this.fail(t.pos, `Unexpected "${t.value}"`);
    }
    return node;
  }

  /** additive = multiplicative (('+' | '-') multiplicative)* */
  private parseAdditive(): AstNode {
    let left = this.parseMultiplicative();
    while (true) {
      const t = this.peek();
      if (t.kind === "op" && (t.value === "+" || t.value === "-")) {
        this.advance();
        const right = this.parseMultiplicative();
        left = { kind: "binary", op: t.value as "+" | "-", left, right, pos: t.pos };
      } else {
        return left;
      }
    }
  }

  /** multiplicative = unary (('*' | '/' | 'mod' | implicit-×) unary)* */
  private parseMultiplicative(): AstNode {
    let left = this.parseUnary();
    while (true) {
      const t = this.peek();
      if (t.kind === "op" && (t.value === "*" || t.value === "/")) {
        this.advance();
        const right = this.parseUnary();
        left = { kind: "binary", op: t.value as "*" | "/", left, right, pos: t.pos };
      } else if (t.kind === "mod") {
        this.advance();
        const right = this.parseUnary();
        left = { kind: "binary", op: "mod", left, right, pos: t.pos };
      } else if (this.startsImplicitMultiplication(t)) {
        // implicit multiplication gets no operator token; reuse left's pos
        const right = this.parseUnary();
        left = { kind: "binary", op: "*", left, right, pos: left.pos };
      } else {
        return left;
      }
    }
  }

  private startsImplicitMultiplication(t: Token): boolean {
    return t.kind === "number" || t.kind === "ident" || t.kind === "lparen";
  }

  /** unary = ('+' | '-') unary | power */
  private parseUnary(): AstNode {
    const t = this.peek();
    if (t.kind === "op" && (t.value === "+" || t.value === "-")) {
      this.advance();
      const operand = this.parseUnary();
      return { kind: "unary", op: t.value as "+" | "-", operand, pos: t.pos };
    }
    return this.parsePower();
  }

  /** power = postfix ('^' unary)?  — right-assoc, exponent may be unary (2^-3) */
  private parsePower(): AstNode {
    const base = this.parsePostfix();
    const t = this.peek();
    if (t.kind === "op" && t.value === "^") {
      this.advance();
      const exponent = this.parseUnary();
      return { kind: "binary", op: "^", left: base, right: exponent, pos: t.pos };
    }
    return base;
  }

  /** postfix = primary ('!' )*  (postfix % was already classified by the lexer) */
  private parsePostfix(): AstNode {
    let node = this.parsePrimary();
    while (true) {
      const t = this.peek();
      if (t.kind === "op" && t.value === "!") {
        this.advance();
        node = { kind: "factorial", operand: node, pos: t.pos };
      } else if (t.kind === "percent") {
        this.advance();
        node = { kind: "percent", operand: node, pos: t.pos };
      } else {
        return node;
      }
    }
  }

  /** primary = number | constant | function '(' expr ')' | '(' expr ')' */
  private parsePrimary(): AstNode {
    const t = this.peek();

    if (t.kind === "number") {
      this.advance();
      return { kind: "number", value: t.value, pos: t.pos };
    }

    if (t.kind === "ident") {
      this.advance();
      if ((t.value === "pi" || t.value === "e")) {
        return { kind: "constant", name: t.value, pos: t.pos };
      }
      // function call — an opening parenthesis is required
      const open = this.peek();
      if (open.kind !== "lparen") {
        this.fail(open.pos, `Expected "(" after ${t.value}`);
      }
      this.advance();
      this.openParens++;
      const arg = this.parseAdditive();
      const close = this.peek();
      if (close.kind === "rparen") {
        this.advance();
        this.openParens--;
      } else if (close.kind === "end") {
        // tolerate unclosed function call here; evaluator reports
        // UnbalancedParentheses so the UI can highlight it while typing
      } else {
        this.fail(close.pos, `Expected ")" in ${t.value}(...)`);
      }
      return { kind: "call", name: t.value, arg, pos: t.pos };
    }

    if (t.kind === "lparen") {
      this.advance();
      this.openParens++;
      const inner = this.parseAdditive();
      const close = this.peek();
      if (close.kind === "rparen") {
        this.advance();
        this.openParens--;
      } else if (close.kind !== "end") {
        this.fail(close.pos, 'Expected ")"');
      }
      // Group wrapper: a % inside parentheses evaluates plain, and
      // `200 + (10%)` is NOT percent-of-total (unlike `200 + 10%`).
      return { kind: "group", inner, pos: t.pos };
    }

    if (t.kind === "end") {
      this.fail(t.pos, "Unexpected end of expression");
    }
    this.fail(t.pos, `Unexpected "${t.value}"`);
  }
}

/**
 * Parse `input` into an AST. Throws ParseFailure on syntax errors.
 * `openParens` > 0 means the expression has unclosed '(' — callers can
 * surface UnbalancedParentheses.
 */
export function parse(input: string): ParseOutput {
  try {
    const parser = new Parser(input);
    const ast = parser.parse();
    return { ast, openParens: parser.openParens };
  } catch (e) {
    if (e instanceof LexFailure || e instanceof ParseFailure) {
      throw e;
    }
    throw new ParseFailure(syntaxError(0, "Invalid expression"));
  }
}
