// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2025 toadCALCULATOR contributors
"use client";

import { useMemo, useState } from "react";
import Decimal from "decimal.js";
import { AnimatePresence, motion } from "framer-motion";
import { Check, Copy } from "lucide-react";
import { toast } from "sonner";
import { evaluate, formatNumber } from "@/lib/math";
import { useCalcStore } from "@/store/calculator";
import { useHydrated } from "@/hooks/use-calc-hooks";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

/**
 * The calculator display: expression on top, live preview beneath,
 * inline typed errors (never toasts, never crashes), result state after `=`.
 */
export function CalculatorDisplay() {
  const hydrated = useHydrated();
  const expression = useCalcStore((s) => s.expression);
  const justEvaluated = useCalcStore((s) => s.justEvaluated);
  const lastResultPlain = useCalcStore((s) => s.lastResultPlain);
  const displayedError = useCalcStore((s) => s.displayedError);
  const angleMode = useCalcStore((s) => s.angleMode);
  const precision = useCalcStore((s) => s.precision);
  const groupThousands = useCalcStore((s) => s.groupThousands);
  const [copied, setCopied] = useState(false);

  // live preview: silent while typing (syntax/unbalanced errors stay hidden)
  const preview = useMemo(() => {
    if (!hydrated || justEvaluated || expression.trim() === "") return null;
    const r = evaluate(expression, angleMode);
    if (!r.ok) return null;
    return formatNumber(r.value, { precision, groupThousands });
  }, [hydrated, justEvaluated, expression, angleMode, precision, groupThousands]);

  // after `=` the stored plain result is re-formatted on precision change
  // (re-format without recompute, mirroring the Android behavior)
  const finalResult = useMemo(() => {
    if (!hydrated || !justEvaluated || lastResultPlain === null) return null;
    try {
      return formatNumber(new Decimal(lastResultPlain), { precision, groupThousands });
    } catch {
      return lastResultPlain;
    }
  }, [hydrated, justEvaluated, lastResultPlain, precision, groupThousands]);

  const resultText = finalResult ?? lastResultPlain ?? "";

  const copyResult = async () => {
    if (!resultText) return;
    try {
      await navigator.clipboard.writeText(resultText);
      setCopied(true);
      toast("Result copied to clipboard");
      setTimeout(() => setCopied(false), 1500);
    } catch {
      toast("Could not copy", { description: "Clipboard access was denied." });
    }
  };

  const exprSize =
    expression.length > 26
      ? "text-2xl"
      : expression.length > 16
        ? "text-3xl"
        : "text-4xl sm:text-5xl";

  return (
    <div
      className="rounded-2xl border bg-card px-4 pb-3 pt-2.5 shadow-sm"
      role="status"
      aria-live="polite"
      aria-label="Calculator display"
    >
      {/* expression */}
      <div className="min-h-10 overflow-x-auto text-right">
        <p
          className={cn(
            "whitespace-nowrap font-medium tabular-nums tracking-tight",
            exprSize,
            justEvaluated ? "text-foreground" : "text-foreground/90",
          )}
        >
          {hydrated ? expression : ""}
        </p>
      </div>

      {/* preview / result / error */}
      <div className="mt-0.5 flex min-h-8 items-center justify-end">
        <AnimatePresence mode="wait" initial={false}>
          {displayedError && !justEvaluated ? (
            <motion.div
              key="error"
              initial={{ opacity: 0, x: 0 }}
              animate={{ opacity: 1, x: [0, -7, 7, -4, 4, 0] }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.35 }}
              className="flex items-center gap-2 text-right"
              role="alert"
            >
              <span className="rounded-md bg-destructive/10 px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-destructive">
                {displayedError.type}
              </span>
              <span className="text-sm text-destructive">
                {displayedError.message}
              </span>
            </motion.div>
          ) : finalResult !== null ? (
            <motion.div
              key="result"
              initial={{ opacity: 0, y: 6 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0 }}
              className="flex items-center gap-2"
            >
              <span className="max-w-[16rem] truncate text-2xl font-semibold tabular-nums text-primary sm:text-3xl">
                {finalResult}
              </span>
              <Button
                variant="ghost"
                size="icon"
                className="h-7 w-7"
                onClick={copyResult}
                aria-label="Copy result"
                title="Copy result"
              >
                {copied ? (
                  <Check className="h-4 w-4 text-primary" aria-hidden />
                ) : (
                  <Copy className="h-4 w-4" aria-hidden />
                )}
              </Button>
            </motion.div>
          ) : preview !== null ? (
            <motion.span
              key="preview"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="max-w-full truncate text-xl tabular-nums text-muted-foreground"
            >
              {preview}
            </motion.span>
          ) : (
            <span key="blank" className="text-xl" aria-hidden>
              &nbsp;
            </span>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}
