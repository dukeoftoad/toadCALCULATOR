// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2025 toadCALCULATOR contributors
"use client";

import { useMemo, useState } from "react";
import { dateDiff, parseYMD } from "@/lib/tools/datetime";
import { useTodayIso } from "@/hooks/use-calc-hooks";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";

function yearAgo(isoToday: string): string {
  const t = parseYMD(isoToday);
  if (!t) return "";
  return `${t.y - 1}-${String(t.m).padStart(2, "0")}-${String(t.d).padStart(2, "0")}`;
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl bg-secondary/60 px-3 py-2.5">
      <p className="text-[11px] uppercase tracking-wide text-muted-foreground">{label}</p>
      <p className="text-lg font-semibold tabular-nums">{value}</p>
    </div>
  );
}

export function DateDiffTool() {
  const todayIso = useTodayIso();
  const [startOverride, setStart] = useState<string | null>(null);
  const [end, setEnd] = useState("");
  const [inclusive, setInclusive] = useState(false);
  const start = startOverride ?? yearAgo(todayIso);

  const result = useMemo(() => {
    const s = parseYMD(start);
    const e = end ? parseYMD(end) : null;
    if (!s || !e) return { ok: false as const, message: "Pick both dates." };
    return dateDiff(s, e, inclusive);
  }, [start, end, inclusive]);

  return (
    <div className="flex flex-col gap-4">
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <div className="space-y-1.5">
          <Label htmlFor="dd-start" className="text-xs text-muted-foreground">Start date</Label>
          <Input
            id="dd-start"
            type="date"
            value={start}
            onChange={(e) => setStart(e.target.value)}
            className="h-11"
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="dd-end" className="text-xs text-muted-foreground">End date</Label>
          <Input
            id="dd-end"
            type="date"
            value={end}
            onChange={(e) => setEnd(e.target.value)}
            className="h-11"
          />
        </div>
      </div>

      <div className="flex items-center justify-between rounded-xl border px-3 py-2.5">
        <div>
          <Label htmlFor="dd-inclusive" className="text-sm font-medium">
            Include end date
          </Label>
          <p className="text-xs text-muted-foreground">
            Count the end date itself (inclusive interval).
          </p>
        </div>
        <Switch
          id="dd-inclusive"
          checked={inclusive}
          onCheckedChange={setInclusive}
          aria-label="Include end date"
        />
      </div>

      <div className="rounded-2xl border bg-card px-4 py-4" aria-live="polite">
        {result.ok ? (
          <div className="space-y-3">
            <div>
              <p className="text-xs text-muted-foreground">
                Total days {inclusive ? "(inclusive)" : ""}
              </p>
              <p className="text-3xl font-semibold tabular-nums text-primary">
                {result.totalDays!.toLocaleString()}
              </p>
            </div>
            <div className="grid grid-cols-2 gap-2">
              <Stat
                label="Weeks"
                value={
                  result.weekRemainder === 0
                    ? `${result.weeks!.toLocaleString()} w`
                    : `${result.weeks!.toLocaleString()} w ${result.weekRemainder} d`
                }
              />
              <Stat
                label="Months"
                value={
                  result.monthRemainder === 0
                    ? `${result.months!.toLocaleString()} mo`
                    : `${result.months!.toLocaleString()} mo ${result.monthRemainder} d`
                }
              />
            </div>
            <p className="text-sm text-muted-foreground">
              Calendar span:{" "}
              <span className="font-medium text-foreground">
                {result.years} {result.years === 1 ? "year" : "years"},{" "}
                {result.monthsOfYearSpan} {result.monthsOfYearSpan === 1 ? "month" : "months"},{" "}
                {result.daysOfYearSpan} {result.daysOfYearSpan === 1 ? "day" : "days"}
              </span>
            </p>
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">{result.message}</p>
        )}
      </div>
    </div>
  );
}
