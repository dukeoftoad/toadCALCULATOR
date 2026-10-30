// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2025 toadCALCULATOR contributors
"use client";

import { useMemo, useState } from "react";
import { computeBmi, CATEGORY_TONE } from "@/lib/tools/bmi";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { cn } from "@/lib/utils";

export function BmiCalculator() {
  const [system, setSystem] = useState<"metric" | "imperial">("metric");
  const [cm, setCm] = useState("170");
  const [ft, setFt] = useState("5");
  const [inch, setInch] = useState("7");
  const [kg, setKg] = useState("65");
  const [lb, setLb] = useState("143");

  const outcome = useMemo(() => {
    let heightM: number | null = null;
    let weightKg: number | null = null;
    let unit: "kg" | "lb" = "kg";

    if (system === "metric") {
      heightM = cm.trim() === "" ? null : Number(cm) / 100;
      weightKg = kg.trim() === "" ? null : Number(kg);
      unit = "kg";
    } else {
      const f = ft.trim() === "" ? 0 : Number(ft);
      const i = inch.trim() === "" ? 0 : Number(inch);
      heightM = f === 0 && i === 0 ? null : f * 0.3048 + i * 0.0254;
      weightKg = lb.trim() === "" ? null : Number(lb) * 0.45359237;
      unit = "lb";
    }
    return computeBmi(heightM, weightKg, unit);
  }, [system, cm, ft, inch, kg, lb]);

  const unitLabel = system === "metric" ? "kg" : "lb";

  return (
    <div className="flex flex-col gap-4">
      <Tabs value={system} onValueChange={(v) => setSystem(v as "metric" | "imperial")}>
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="metric">Metric</TabsTrigger>
          <TabsTrigger value="imperial">Imperial</TabsTrigger>
        </TabsList>
      </Tabs>

      {system === "metric" ? (
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1.5">
            <Label htmlFor="bmi-cm" className="text-xs text-muted-foreground">Height (cm)</Label>
            <Input
              id="bmi-cm"
              inputMode="decimal"
              value={cm}
              onChange={(e) => setCm(e.target.value)}
              className="h-11 tabular-nums"
              placeholder="170"
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="bmi-kg" className="text-xs text-muted-foreground">Weight (kg)</Label>
            <Input
              id="bmi-kg"
              inputMode="decimal"
              value={kg}
              onChange={(e) => setKg(e.target.value)}
              className="h-11 tabular-nums"
              placeholder="65"
            />
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-3 gap-3">
          <div className="space-y-1.5">
            <Label htmlFor="bmi-ft" className="text-xs text-muted-foreground">Height (ft)</Label>
            <Input
              id="bmi-ft"
              inputMode="numeric"
              value={ft}
              onChange={(e) => setFt(e.target.value)}
              className="h-11 tabular-nums"
              placeholder="5"
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="bmi-in" className="text-xs text-muted-foreground">(in)</Label>
            <Input
              id="bmi-in"
              inputMode="decimal"
              value={inch}
              onChange={(e) => setInch(e.target.value)}
              className="h-11 tabular-nums"
              placeholder="7"
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="bmi-lb" className="text-xs text-muted-foreground">Weight (lb)</Label>
            <Input
              id="bmi-lb"
              inputMode="decimal"
              value={lb}
              onChange={(e) => setLb(e.target.value)}
              className="h-11 tabular-nums"
              placeholder="143"
            />
          </div>
        </div>
      )}

      <div className="rounded-2xl border bg-card px-4 py-4" aria-live="polite">
        {outcome.ok ? (
          <div className="space-y-3">
            <div className="flex items-baseline justify-between gap-3">
              <div>
                <p className="text-xs text-muted-foreground">Your BMI</p>
                <p className="text-4xl font-semibold tabular-nums text-primary">
                  {outcome.bmi!.toFixed(1)}
                </p>
              </div>
              <span
                className={cn(
                  "rounded-full px-3 py-1 text-sm font-semibold",
                  CATEGORY_TONE[outcome.category!],
                )}
              >
                {outcome.category}
              </span>
            </div>
            <p className="text-sm text-muted-foreground">
              Healthy weight for your height:{" "}
              <span className="font-medium tabular-nums text-foreground">
                {outcome.rangeLow} – {outcome.rangeHigh} {unitLabel}
              </span>{" "}
              (WHO classification).
            </p>
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">{outcome.message}</p>
        )}
      </div>
    </div>
  );
}
