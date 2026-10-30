// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2025 toadCALCULATOR contributors
"use client";

import { useMemo, useState } from "react";
import { Cake } from "lucide-react";
import {
  ageBreakdown,
  nextBirthday,
  parseYMD,
  type YMD,
} from "@/lib/tools/datetime";
import { useTodayIso } from "@/hooks/use-calc-hooks";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";

function iso(ymd: YMD): string {
  return `${String(ymd.y).padStart(4, "0")}-${String(ymd.m).padStart(2, "0")}-${String(ymd.d).padStart(2, "0")}`;
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl bg-secondary/60 px-3 py-2.5">
      <p className="text-[11px] uppercase tracking-wide text-muted-foreground">{label}</p>
      <p className="text-lg font-semibold tabular-nums">{value}</p>
    </div>
  );
}

export function AgeCalculator() {
  const [birth, setBirth] = useState("2000-01-01");
  const [asOfOverride, setAsOf] = useState<string | null>(null);
  const todayIso = useTodayIso();
  const asOf = asOfOverride ?? todayIso;

  const outcome = useMemo(() => {
    const b = parseYMD(birth);
    const a = asOf ? parseYMD(asOf) : null;
    if (!b || !a) return { ok: false as const, message: "Pick both dates." };
    const age = ageBreakdown(b, a);
    if (!age.ok) return { ok: false as const, message: age.message! };
    return { ok: true as const, age, birthday: nextBirthday(b, a) };
  }, [birth, asOf]);

  return (
    <div className="flex flex-col gap-4">
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <div className="space-y-1.5">
          <Label htmlFor="age-birth" className="text-xs text-muted-foreground">
            Date of birth
          </Label>
          <Input
            id="age-birth"
            type="date"
            value={birth}
            max={asOf || undefined}
            onChange={(e) => setBirth(e.target.value)}
            className="h-11"
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="age-asof" className="text-xs text-muted-foreground">
            Age at date
          </Label>
          <Input
            id="age-asof"
            type="date"
            value={asOf}
            onChange={(e) => setAsOf(e.target.value)}
            className="h-11"
          />
        </div>
      </div>

      <div className="rounded-2xl border bg-card px-4 py-4" aria-live="polite">
        {outcome.ok ? (
          <div className="space-y-3">
            <div>
              <p className="text-xs text-muted-foreground">Age</p>
              <p className="text-2xl font-semibold tabular-nums text-primary">
                {outcome.age.years} years, {outcome.age.months} months,{" "}
                {outcome.age.days} days
              </p>
            </div>
            <div className="grid grid-cols-3 gap-2">
              <Stat label="Total days" value={outcome.age.totalDays!.toLocaleString()} />
              <Stat label="Total weeks" value={outcome.age.totalWeeks!.toLocaleString()} />
              <Stat label="Total months" value={outcome.age.totalMonths!.toLocaleString()} />
            </div>
            <div className="flex items-center gap-2 rounded-xl bg-primary/10 px-3 py-2.5 text-sm">
              <Cake className="h-4 w-4 shrink-0 text-primary" aria-hidden />
              {outcome.birthday.isToday ? (
                <span className="font-medium text-primary">
                  It&apos;s your birthday today!
                </span>
              ) : (
                <span className="text-muted-foreground">
                  Next birthday in{" "}
                  <span className="font-semibold tabular-nums text-foreground">
                    {outcome.birthday.daysUntil}
                  </span>{" "}
                  {outcome.birthday.daysUntil === 1 ? "day" : "days"} ·{" "}
                  {iso(outcome.birthday.date)}
                </span>
              )}
            </div>
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">{outcome.message}</p>
        )}
      </div>
    </div>
  );
}
