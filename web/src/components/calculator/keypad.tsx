// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2025 toadCALCULATOR contributors
"use client";

import { AnimatePresence, motion } from "framer-motion";
import { Delete } from "lucide-react";
import { useCalcStore, vibrate } from "@/store/calculator";
import { useHydrated, useLongPress } from "@/hooks/use-calc-hooks";
import { cn } from "@/lib/utils";

type Variant = "digit" | "action" | "operator" | "equals" | "fn" | "danger";

interface KeyDef {
  label: string;
  token?: string;
  variant: Variant;
  aria: string;
  /** keyboard equivalent for desktop input */
  keys?: string;
}

const VARIANT_CLASSES: Record<Variant, string> = {
  digit: "bg-card text-foreground border shadow-sm hover:bg-accent",
  action: "bg-secondary text-secondary-foreground hover:bg-secondary/70",
  operator: "bg-primary/10 text-primary hover:bg-primary/20",
  equals: "bg-primary text-primary-foreground hover:bg-primary/90 shadow-sm",
  fn: "bg-secondary text-secondary-foreground text-sm hover:bg-secondary/70",
  danger: "bg-destructive/10 text-destructive hover:bg-destructive/20",
};

/** scientific rows; `second` swaps each key to its secondary label/token */
const SCI_ROW_1 = [
  { label: "sin", alt: "sin⁻¹", token: "sin(", altToken: "asin(", aria: "sine", altAria: "arcsine" },
  { label: "cos", alt: "cos⁻¹", token: "cos(", altToken: "acos(", aria: "cosine", altAria: "arccosine" },
  { label: "tan", alt: "tan⁻¹", token: "tan(", altToken: "atan(", aria: "tangent", altAria: "arctangent" },
  { label: "ln", alt: "eˣ", token: "ln(", altToken: "exp(", aria: "natural logarithm", altAria: "exponential" },
];
const SCI_ROW_2 = [
  { label: "log", alt: "log₂", token: "log(", altToken: "log2(", aria: "common logarithm", altAria: "logarithm base 2" },
  { label: "√", alt: "∛", token: "sqrt(", altToken: "cbrt(", aria: "square root", altAria: "cube root" },
  { label: "^", alt: "^", token: "^", altToken: "^", aria: "power", altAria: "power" },
  { label: "!", alt: "|x|", token: "!", altToken: "abs(", aria: "factorial", altAria: "absolute value" },
];
const SCI_ROW_3 = [
  { label: "π", token: "π", aria: "pi" },
  { label: "e", token: "e", aria: "Euler's number" },
  { label: "%", token: "%", aria: "percent" },
];

const BASIC_GRID: (KeyDef | null)[] = [
  { label: "AC", variant: "danger", aria: "All clear", keys: "Escape" },
  { label: "⌫", variant: "action", aria: "Delete — long-press to clear all", keys: "Backspace" },
  { label: "(", token: "(", variant: "action", aria: "Left parenthesis", keys: "(" },
  { label: ")", token: ")", variant: "action", aria: "Right parenthesis", keys: ")" },
  { label: "÷", token: "÷", variant: "operator", aria: "Divide", keys: "/" },
  { label: "7", token: "7", variant: "digit", aria: "Seven", keys: "7" },
  { label: "8", token: "8", variant: "digit", aria: "Eight", keys: "8" },
  { label: "9", token: "9", variant: "digit", aria: "Nine", keys: "9" },
  { label: "×", token: "×", variant: "operator", aria: "Multiply", keys: "*" },
  { label: "4", token: "4", variant: "digit", aria: "Four", keys: "4" },
  { label: "5", token: "5", variant: "digit", aria: "Five", keys: "5" },
  { label: "6", token: "6", variant: "digit", aria: "Six", keys: "6" },
  { label: "−", token: "-", variant: "operator", aria: "Subtract", keys: "-" },
  { label: "1", token: "1", variant: "digit", aria: "One", keys: "1" },
  { label: "2", token: "2", variant: "digit", aria: "Two", keys: "2" },
  { label: "3", token: "3", variant: "digit", aria: "Three", keys: "3" },
  { label: "+", token: "+", variant: "operator", aria: "Add", keys: "+" },
  { label: "0", token: "0", variant: "digit", aria: "Zero", keys: "0" },
  { label: ".", token: ".", variant: "digit", aria: "Decimal point", keys: "." },
  { label: "=", variant: "equals", aria: "Equals", keys: "Enter" },
];

export function CalculatorKeypad() {
  const hydrated = useHydrated();
  const input = useCalcStore((s) => s.input);
  const backspace = useCalcStore((s) => s.backspace);
  const clearAll = useCalcStore((s) => s.clearAll);
  const equals = useCalcStore((s) => s.equals);
  const scientific = useCalcStore((s) => s.scientific);
  const setScientific = useCalcStore((s) => s.setScientific);
  const second = useCalcStore((s) => s.second);
  const setSecond = useCalcStore((s) => s.setSecond);
  const angleMode = useCalcStore((s) => s.angleMode);
  const setAngleMode = useCalcStore((s) => s.setAngleMode);
  const haptics = useCalcStore((s) => s.haptics);

  const long = useLongPress(() => {
    vibrate(haptics, [20, 40, 20]);
    clearAll();
  });

  if (!hydrated) {
    return <div className="h-[24rem] animate-pulse rounded-2xl bg-muted/40" aria-hidden />;
  }

  const press = (fn: () => void) => () => {
    vibrate(haptics);
    fn();
  };

  const sciKey = (
    def: { label: string; alt?: string; token: string; altToken?: string; aria: string; altAria?: string },
  ) => {
    const useAlt = second && def.altToken !== undefined;
    return (
      <button
        key={useAlt ? def.altToken : def.token + def.label}
        type="button"
        onClick={press(() => input(useAlt ? def.altToken! : def.token))}
        className={cn(
          "flex h-12 items-center justify-center rounded-xl font-medium transition-colors",
          VARIANT_CLASSES.fn,
          useAlt && "bg-primary/10 text-primary",
        )}
        aria-label={useAlt ? def.altAria : def.aria}
        title={useAlt ? def.altAria : def.aria}
      >
        <AnimatePresence mode="wait" initial={false}>
          <motion.span
            key={useAlt ? "alt" : "prim"}
            initial={{ opacity: 0, y: 4 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -4 }}
            transition={{ duration: 0.12 }}
          >
            {useAlt ? def.alt : def.label}
          </motion.span>
        </AnimatePresence>
      </button>
    );
  };

  return (
    <div className="flex flex-col gap-2" aria-label="Calculator keypad">
      {/* mode toolbar: SCI, 2ND and DEG/RAD chips share one row */}
      <div className="flex items-center justify-between gap-2">
        <button
          type="button"
          onClick={press(() => setScientific(!scientific))}
          aria-pressed={scientific}
          className={cn(
            "flex h-9 items-center rounded-full px-4 text-xs font-bold tracking-wider transition-colors",
            scientific
              ? "bg-primary text-primary-foreground"
              : "bg-secondary text-secondary-foreground hover:bg-secondary/70",
          )}
          aria-label={scientific ? "Disable scientific mode" : "Enable scientific mode"}
          title="Scientific functions: trig, logarithms, roots, powers, constants"
        >
          SCI
        </button>
        {scientific ? (
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={press(() => setSecond(!second))}
              aria-pressed={second}
              className={cn(
                "flex h-9 items-center rounded-full px-4 text-xs font-bold tracking-wider transition-colors",
                second
                  ? "bg-primary text-primary-foreground"
                  : "bg-secondary text-secondary-foreground hover:bg-secondary/70",
              )}
              aria-label="Toggle inverse and secondary functions"
              title="Reveals inverse trig, eˣ, log₂, ∛, |x|"
            >
              2ND
            </button>
            <button
              type="button"
              onClick={press(() => setAngleMode(angleMode === "deg" ? "rad" : "deg"))}
              className="flex h-9 items-center rounded-full bg-secondary px-4 text-xs font-bold tracking-wider text-secondary-foreground transition-colors hover:bg-secondary/70"
              aria-label={`Angle mode: ${angleMode === "deg" ? "degrees" : "radians"} — tap to switch`}
              title="Switch angle mode"
            >
              {angleMode === "deg" ? "DEG" : "RAD"}
            </button>
          </div>
        ) : (
          <span className="text-xs text-muted-foreground">SCI adds scientific keys</span>
        )}
      </div>

      {/* scientific block — animated expand/collapse, expression preserved */}
      <AnimatePresence initial={false}>
        {scientific && (
          <motion.div
            key="sci"
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.25, ease: "easeInOut" }}
            className="overflow-hidden"
          >
            <div className="pb-1">
              <div className="grid grid-cols-4 gap-2">
                {SCI_ROW_1.map(sciKey)}
                {SCI_ROW_2.map(sciKey)}
                {SCI_ROW_3.map((d) => (
                  <button
                    key={d.token}
                    type="button"
                    onClick={press(() => input(d.token))}
                    className={cn(
                      "flex h-12 items-center justify-center rounded-xl text-sm font-medium transition-colors",
                      VARIANT_CLASSES.fn,
                    )}
                    aria-label={d.aria}
                    title={d.aria}
                  >
                    {d.label}
                  </button>
                ))}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* basic grid */}
      <div className="grid grid-cols-4 gap-2">
        {BASIC_GRID.map((k, i) => {
          if (!k) return <span key={i} aria-hidden />;
          const isDelete = k.label === "⌫";
          const isEquals = k.label === "=";
          const isAC = k.label === "AC";
          return (
            <motion.button
              key={k.label + i}
              type="button"
              whileTap={{ scale: 0.94 }}
              transition={{ type: "spring", stiffness: 500, damping: 30 }}
              onClick={(e) => {
                if (isDelete && long.consumeClick()) return;
                vibrate(haptics);
                if (isAC) clearAll();
                else if (isDelete) backspace();
                else if (isEquals) equals();
                else if (k.token) input(k.token);
              }}
              className={cn(
                "flex h-[3.25rem] select-none items-center justify-center rounded-xl text-xl font-medium tabular-nums transition-colors sm:h-14 sm:text-2xl",
                VARIANT_CLASSES[k.variant],
                k.label === "0" && "col-span-2",
                isDelete && long.pressing && "ring-2 ring-destructive/40",
              )}
              aria-label={k.aria}
              title={k.keys ? `${k.aria} (${k.keys})` : k.aria}
              {...(isDelete ? long.handlers : {})}
            >
              {isDelete ? <Delete className="h-5 w-5" aria-hidden /> : k.label}
            </motion.button>
          );
        })}
      </div>
    </div>
  );
}
