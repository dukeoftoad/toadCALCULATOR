// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2025 toadCALCULATOR contributors
//
// Single source of truth for the calculator: expression state, settings,
// history — persisted to localStorage (the web analogue of the Android
// DataStore + Room pairing). One-way data flow: UI dispatches actions,
// components read state.

"use client";

import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import {
  evaluate,
  formatNumber,
  toPlainString,
  type AngleMode,
  type MathError,
} from "@/lib/math";
import {
  backspaceToken,
  endsWithOperator,
  openParenBalance,
  trailingNumber,
} from "@/lib/calc/editing";

export interface HistoryEntry {
  id: string;
  expression: string;
  result: string;
  timestamp: number;
}

export type Precision = 6 | 8 | 10 | 12 | 15;

const FUNCTIONS_TOKENS = [
  "asin(",
  "acos(",
  "atan(",
  "sinh(",
  "cosh(",
  "tanh(",
  "log2(",
  "sqrt(",
  "cbrt(",
  "abs(",
  "exp(",
  "floor(",
  "ceil(",
  "round(",
  "sin(",
  "cos(",
  "tan(",
  "ln(",
  "log(",
];

const MAX_HISTORY = 200;

export interface CalcState {
  expression: string;
  justEvaluated: boolean;
  /** plain parseable result of the last successful `=` (ans-chaining) */
  lastResultPlain: string | null;
  /** inline error rendered after a failed `=` — never a toast, never a crash */
  displayedError: MathError | null;

  scientific: boolean;
  second: boolean;

  // settings
  angleMode: AngleMode;
  precision: Precision;
  groupThousands: boolean;
  haptics: boolean;

  history: HistoryEntry[];

  input: (token: string) => void;
  backspace: () => void;
  clearAll: () => void;
  equals: () => void;
  loadFromHistory: (entry: HistoryEntry) => void;
  deleteHistory: (id: string) => void;
  clearHistory: () => void;

  setScientific: (v: boolean) => void;
  setSecond: (v: boolean) => void;
  setAngleMode: (m: AngleMode) => void;
  setPrecision: (p: Precision) => void;
  setGroupThousands: (v: boolean) => void;
  setHaptics: (v: boolean) => void;
}

export const useCalcStore = create<CalcState>()(
  persist(
    (set, get) => ({
      expression: "",
      justEvaluated: false,
      lastResultPlain: null,
      displayedError: null,

      scientific: false,
      second: false,

      angleMode: "deg",
      precision: 12,
      groupThousands: true,
      haptics: true,

      history: [],

      input: (token) => {
        const s = get();
        let expr = s.expression;

        const startsOperand =
          /^\d$/.test(token) ||
          token === "." ||
          token === "π" ||
          token === "e" ||
          FUNCTIONS_TOKENS.includes(token) ||
          token === "(";

        // after `=`: digits/constants/functions start fresh, operators chain
        if (s.justEvaluated) {
          if (startsOperand) {
            expr = "";
          } else if (s.lastResultPlain !== null && !token.startsWith(")")) {
            expr = s.lastResultPlain;
          } else {
            return;
          }
        }

        // operator handling: replace a trailing operator, allow leading minus
        if (token === "+" || token === "-" || token === "×" || token === "÷" || token === "^") {
          if (expr === "") {
            if (token !== "-") return; // only a leading minus may seed
            set({ expression: "-", justEvaluated: false, displayedError: null });
            return;
          }
          if (endsWithOperator(expr)) {
            expr = expr.slice(0, -1);
          }
          set({ expression: expr + token, justEvaluated: false, displayedError: null });
          return;
        }

        // '.' inside the current number run only
        if (token === ".") {
          const tail = trailingNumber(expr);
          if (tail.includes(".")) return;
          // a bare '.' after an operator/empty becomes "0."
          if (tail === "" && !/[\d)]$|π$|e$/.test(expr)) {
            set({ expression: expr + "0.", justEvaluated: false, displayedError: null });
            return;
          }
          set({ expression: expr + ".", justEvaluated: false, displayedError: null });
          return;
        }

        // ')' requires an open parenthesis
        if (token === ")") {
          if (openParenBalance(expr) <= 0) return;
          if (/[+\-×÷^(]$/.test(expr)) return;
          set({ expression: expr + ")", justEvaluated: false, displayedError: null });
          return;
        }

        // postfix % / ! require something to attach to
        if (token === "%" || token === "!") {
          if (!/[\d)%!]$|π$|e$/.test(expr)) return;
          set({ expression: expr + token, justEvaluated: false, displayedError: null });
          return;
        }

        // a digit directly after ')' or π/e implies multiplication visually;
        // the parser handles implicit multiplication, so plain append is fine
        set({ expression: expr + token, justEvaluated: false, displayedError: null });
      },

      backspace: () => {
        const s = get();
        if (s.justEvaluated) {
          // backspace after '=' starts a clean slate
          set({ expression: "", justEvaluated: false, displayedError: null, lastResultPlain: s.lastResultPlain });
          return;
        }
        set({ expression: backspaceToken(s.expression), displayedError: null });
      },

      clearAll: () => {
        set({ expression: "", justEvaluated: false, displayedError: null });
      },

      equals: () => {
        const s = get();
        if (s.expression.trim() === "") return;

        const result = evaluate(s.expression, s.angleMode);
        if (result.ok) {
          const formatted = formatNumber(result.value, {
            precision: s.precision,
            groupThousands: s.groupThousands,
          });
          const prev = s.history[0];
          const entry: HistoryEntry = {
            id: crypto.randomUUID(),
            expression: s.expression,
            result: formatted,
            timestamp: Date.now(),
          };
          // failed expressions never persist; identical consecutive results dedupe
          const history =
            prev && prev.expression === entry.expression && prev.result === entry.result
              ? s.history
              : [entry, ...s.history].slice(0, MAX_HISTORY);

          set({
            history,
            lastResultPlain: toPlainString(result.value, s.precision),
            justEvaluated: true,
            displayedError: null,
          });
        } else {
          // inline error state — no crash, no toast, no history write
          set({ displayedError: result.error, justEvaluated: false });
        }
      },

      loadFromHistory: (entry) => {
        set({ expression: entry.expression, justEvaluated: false, displayedError: null });
      },

      deleteHistory: (id) => {
        set({ history: get().history.filter((h) => h.id !== id) });
      },

      clearHistory: () => {
        set({ history: [] });
      },

      setScientific: (v) => set({ scientific: v }),
      setSecond: (v) => set({ second: v }),
      setAngleMode: (m) => set({ angleMode: m }),
      setPrecision: (p) => set({ precision: p }),
      setGroupThousands: (v) => set({ groupThousands: v }),
      setHaptics: (v) => set({ haptics: v }),
    }),
    {
      name: "toadcalculator-v1",
      storage: createJSONStorage(() => localStorage),
      version: 1,
      partialize: (s) => ({
        expression: s.expression,
        lastResultPlain: s.lastResultPlain,
        scientific: s.scientific,
        angleMode: s.angleMode,
        precision: s.precision,
        groupThousands: s.groupThousands,
        haptics: s.haptics,
        history: s.history,
      }),
    },
  ),
);

/** Fire-and-forget haptic feedback (respects the user setting). */
export function vibrate(enabled: boolean, pattern: number | number[] = 8) {
  if (enabled && typeof navigator !== "undefined" && "vibrate" in navigator) {
    try {
      navigator.vibrate(pattern);
    } catch {
      // ignore — haptics are best-effort
    }
  }
}
