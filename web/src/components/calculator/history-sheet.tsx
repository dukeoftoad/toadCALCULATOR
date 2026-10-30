// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2025 toadCALCULATOR contributors
"use client";

import { useState } from "react";
import { History, Trash2, X } from "lucide-react";
import { useCalcStore, type HistoryEntry } from "@/store/calculator";
import { useHydrated } from "@/hooks/use-calc-hooks";
import { useIsMobile } from "@/hooks/use-mobile";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";

function formatTimestamp(ts: number): string {
  const d = new Date(ts);
  return d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

/**
 * History as a bottom sheet on mobile / side sheet on desktop.
 * Tap a row to load the expression back as editable text.
 */
export function HistorySheet() {
  const hydrated = useHydrated();
  const isMobile = useIsMobile();
  const history = useCalcStore((s) => s.history);
  const loadFromHistory = useCalcStore((s) => s.loadFromHistory);
  const deleteHistory = useCalcStore((s) => s.deleteHistory);
  const clearHistory = useCalcStore((s) => s.clearHistory);
  const [open, setOpen] = useState(false);

  const load = (entry: HistoryEntry) => {
    loadFromHistory(entry);
    setOpen(false);
  };

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <Button variant="ghost" size="icon" aria-label="Open history" title="History">
          <History className="h-5 w-5" aria-hidden />
        </Button>
      </SheetTrigger>
      <SheetContent
        side={isMobile ? "bottom" : "right"}
        className="flex h-[70vh] flex-col rounded-t-2xl sm:h-auto sm:rounded-l-2xl sm:rounded-t-none"
      >
        <SheetHeader className="pb-2">
          <SheetTitle className="flex items-center gap-2">
            <History className="h-4 w-4" aria-hidden /> History
          </SheetTitle>
          <SheetDescription>
            Tap a calculation to load it back into the keypad.
          </SheetDescription>
        </SheetHeader>

        <div className="flex items-center justify-between border-b pb-2 text-xs text-muted-foreground">
          <span>
            {hydrated ? `${history.length} ${history.length === 1 ? "entry" : "entries"}` : "—"}
          </span>
          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button
                variant="ghost"
                size="sm"
                className="h-7 gap-1 px-2 text-xs text-destructive hover:text-destructive"
                disabled={!hydrated || history.length === 0}
                aria-label="Clear all history"
              >
                <Trash2 className="h-3.5 w-3.5" aria-hidden /> Clear all
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>Clear all history?</AlertDialogTitle>
                <AlertDialogDescription>
                  This permanently removes every saved calculation from this device.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>Cancel</AlertDialogCancel>
                <AlertDialogAction
                  className="bg-destructive text-white hover:bg-destructive/90"
                  onClick={() => clearHistory()}
                >
                  Clear
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        </div>

        <ScrollArea className="min-h-0 flex-1 pr-2">
          <ul className="flex flex-col gap-1 py-1">
            {hydrated && history.length === 0 && (
              <li className="px-2 py-10 text-center text-sm text-muted-foreground">
                Calculations appear here — failed ones are never saved.
              </li>
            )}
            {hydrated &&
              history.map((entry) => (
                <li key={entry.id} className="group relative">
                  <button
                    type="button"
                    onClick={() => load(entry)}
                    className="w-full rounded-xl px-3 py-2 pr-10 text-left transition-colors hover:bg-accent focus-visible:bg-accent"
                    aria-label={`Load ${entry.expression} equals ${entry.result}`}
                  >
                    <span className="block truncate text-sm text-muted-foreground">
                      {entry.expression}
                    </span>
                    <span className="flex items-baseline justify-between gap-2">
                      <span className="truncate font-semibold tabular-nums">
                        {entry.result}
                      </span>
                      <span className="shrink-0 text-[10px] text-muted-foreground">
                        {formatTimestamp(entry.timestamp)}
                      </span>
                    </span>
                  </button>
                  <button
                    type="button"
                    onClick={() => deleteHistory(entry.id)}
                    className="absolute right-2 top-1/2 -translate-y-1/2 rounded-full p-1.5 text-muted-foreground opacity-0 transition-opacity hover:bg-destructive/10 hover:text-destructive focus-visible:opacity-100 group-hover:opacity-100"
                    aria-label={`Delete ${entry.expression}`}
                    title="Delete entry"
                  >
                    <X className="h-4 w-4" aria-hidden />
                  </button>
                </li>
              ))}
          </ul>
        </ScrollArea>
      </SheetContent>
    </Sheet>
  );
}
