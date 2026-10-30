// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2025 toadCALCULATOR contributors
"use client";

import { useEffect } from "react";
import { Calculator, Moon, Settings2, Sun, Wrench } from "lucide-react";
import { useTheme } from "next-themes";
import { useCalcStore } from "@/store/calculator";
import { CalculatorDisplay } from "@/components/calculator/display";
import { CalculatorKeypad } from "@/components/calculator/keypad";
import { HistorySheet } from "@/components/calculator/history-sheet";
import { SettingsPanel } from "@/components/calculator/settings-panel";
import { ToolsView } from "@/components/tools/tools-view";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";

function ToadMark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 48 34" className={className} aria-hidden focusable="false">
      <ellipse cx="24" cy="23" rx="19" ry="10" fill="currentColor" />
      <circle cx="13" cy="10" r="7" fill="currentColor" />
      <circle cx="35" cy="10" r="7" fill="currentColor" />
      <circle cx="13" cy="9" r="3" fill="var(--background)" />
      <circle cx="35" cy="9" r="3" fill="var(--background)" />
      <circle cx="13.8" cy="9.8" r="1.4" fill="currentColor" />
      <circle cx="35.8" cy="9.8" r="1.4" fill="currentColor" />
      <path
        d="M15 24 Q24 30 33 24"
        stroke="var(--background)"
        strokeWidth="2.4"
        strokeLinecap="round"
        fill="none"
      />
    </svg>
  );
}

function ThemeToggle() {
  const { resolvedTheme, setTheme } = useTheme();
  const isDark = resolvedTheme === "dark";
  return (
    <Button
      variant="ghost"
      size="icon"
      onClick={() => setTheme(isDark ? "light" : "dark")}
      aria-label={isDark ? "Switch to light theme" : "Switch to dark theme"}
      title={isDark ? "Light theme" : "Dark theme"}
    >
      {isDark ? (
        <Sun className="h-5 w-5" aria-hidden />
      ) : (
        <Moon className="h-5 w-5" aria-hidden />
      )}
    </Button>
  );
}

/** Desktop keyboard support: digits, operators, Enter, Backspace, Escape. */
function useKeyboardInput(active: boolean) {
  const input = useCalcStore((s) => s.input);
  const backspace = useCalcStore((s) => s.backspace);
  const clearAll = useCalcStore((s) => s.clearAll);
  const equals = useCalcStore((s) => s.equals);

  useEffect(() => {
    if (!active) return;
    const onKey = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement | null;
      if (
        target &&
        (target.tagName === "INPUT" ||
          target.tagName === "TEXTAREA" ||
          target.tagName === "SELECT" ||
          target.isContentEditable)
      ) {
        return;
      }
      if (e.metaKey || e.ctrlKey || e.altKey) return;

      const k = e.key;
      if (/^[0-9]$/.test(k)) input(k);
      else if (k === "." || k === ",") input(".");
      else if (k === "+") input("+");
      else if (k === "-") input("-");
      else if (k === "*") input("×");
      else if (k === "/") {
        e.preventDefault();
        input("÷");
      } else if (k === "^") input("^");
      else if (k === "%") input("%");
      else if (k === "!") input("!");
      else if (k === "(") input("(");
      else if (k === ")") input(")");
      else if (k === "Enter" || k === "=") {
        e.preventDefault();
        equals();
      } else if (k === "Backspace") backspace();
      else if (k === "Escape") clearAll();
      else return;
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [active, input, backspace, clearAll, equals]);
}

export function CalculatorApp() {
  useKeyboardInput(true);

  return (
    <div className="flex min-h-screen flex-col bg-background text-foreground">
      {/* header */}
      <header className="sticky top-0 z-20 border-b bg-background/80 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="mx-auto flex h-14 w-full max-w-2xl items-center justify-between gap-2 px-4">
          <div className="flex items-center gap-2.5">
            <ToadMark className="h-6 w-9 text-primary" />
            <div className="leading-tight">
              <p className="text-sm font-bold tracking-tight">toadCALCULATOR</p>
              <p className="text-[10px] uppercase tracking-widest text-muted-foreground">
                offline · no ads · no tracking
              </p>
            </div>
          </div>
          <div className="flex items-center gap-1">
            <ThemeToggle />
            <HistorySheet />
          </div>
        </div>
      </header>

      {/* main */}
      <main className="mx-auto w-full max-w-2xl flex-1 px-4 pb-4 pt-3">
        <Tabs defaultValue="calc">
          <TabsList className="mx-auto grid w-full max-w-sm grid-cols-3">
            <TabsTrigger value="calc" className="gap-1.5">
              <Calculator className="h-4 w-4" aria-hidden /> Calculator
            </TabsTrigger>
            <TabsTrigger value="more" className="gap-1.5">
              <Wrench className="h-4 w-4" aria-hidden /> Tools
            </TabsTrigger>
            <TabsTrigger value="settings" className="gap-1.5">
              <Settings2 className="h-4 w-4" aria-hidden /> Settings
            </TabsTrigger>
          </TabsList>

          <TabsContent value="calc" className="mx-auto mt-2 flex w-full max-w-md flex-col gap-3">
            <CalculatorDisplay />
            <CalculatorKeypad />
          </TabsContent>

          <TabsContent value="more" className="mt-4 flex flex-col gap-4">
            <ToolsView />
          </TabsContent>

          <TabsContent value="settings" className="mt-4">
            <SettingsPanel />
          </TabsContent>
        </Tabs>
      </main>

      {/* sticky footer — sticks to the bottom, pushed naturally on overflow */}
      <footer className="mt-auto border-t bg-background">
        <div className="mx-auto flex w-full max-w-2xl flex-wrap items-center justify-between gap-x-4 gap-y-1 px-4 py-3 text-xs text-muted-foreground">
          <p className="font-medium text-foreground/70">
            toadCALCULATOR v1.0.0
          </p>
          <p>GPL-3.0-or-later · Works offline · No ads · No tracking</p>
        </div>
      </footer>
    </div>
  );
}
