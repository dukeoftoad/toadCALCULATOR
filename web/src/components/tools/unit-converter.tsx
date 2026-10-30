// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2025 toadCALCULATOR contributors
"use client";

import { useState } from "react";
import { ArrowLeftRight } from "lucide-react";
import { UNIT_CATEGORIES, convert } from "@/lib/tools/units";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";

const DEFAULT_FROM: Record<string, string> = {
  length: "m",
  area: "m2",
  volume: "l",
  mass: "kg",
  temperature: "c",
  speed: "kmh",
  time: "min",
  data: "mb",
};

const DEFAULT_TO: Record<string, string> = {
  length: "ft",
  area: "ft2",
  volume: "gal",
  mass: "lb",
  temperature: "f",
  speed: "mph",
  time: "h",
  data: "gb",
};

export function UnitConverter() {
  const [categoryId, setCategoryId] = useState("length");
  const [from, setFrom] = useState(DEFAULT_FROM.length);
  const [to, setTo] = useState(DEFAULT_TO.length);
  const [value, setValue] = useState("1");

  const category = UNIT_CATEGORIES.find((c) => c.id === categoryId)!;

  const result = convert(categoryId, from, to, value);

  const pickCategory = (id: string) => {
    setCategoryId(id);
    setFrom(DEFAULT_FROM[id]);
    setTo(DEFAULT_TO[id]);
  };

  const swap = () => {
    setFrom(to);
    setTo(from);
  };

  const unitOf = (id: string) =>
    category.units.find((u) => u.id === id)?.label ?? id;

  return (
    <div className="flex flex-col gap-4">
      {/* category pills */}
      <div
        className="flex gap-1.5 overflow-x-auto pb-1 calc-scrollbar"
        role="tablist"
        aria-label="Conversion category"
      >
        {UNIT_CATEGORIES.map((c) => (
          <button
            key={c.id}
            type="button"
            role="tab"
            aria-selected={c.id === categoryId}
            onClick={() => pickCategory(c.id)}
            className={cn(
              "shrink-0 rounded-full px-3.5 py-1.5 text-sm font-medium transition-colors",
              c.id === categoryId
                ? "bg-primary text-primary-foreground"
                : "bg-secondary text-secondary-foreground hover:bg-secondary/70",
            )}
          >
            {c.label}
          </button>
        ))}
      </div>

      <div className="grid gap-3">
        <div className="space-y-1.5">
          <Label htmlFor="conv-value" className="text-xs text-muted-foreground">
            Value
          </Label>
          <Input
            id="conv-value"
            inputMode="decimal"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            placeholder="0"
            className="h-12 text-lg tabular-nums"
          />
        </div>

        <div className="flex items-end gap-2">
          <div className="flex-1 space-y-1.5">
            <Label className="text-xs text-muted-foreground">From</Label>
            <Select value={from} onValueChange={setFrom}>
              <SelectTrigger aria-label="Convert from unit">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {category.units.map((u) => (
                  <SelectItem key={u.id} value={u.id}>
                    {u.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <Button
            variant="outline"
            size="icon"
            className="mb-0.5 h-10 w-10 shrink-0"
            onClick={swap}
            aria-label="Swap units"
            title="Swap units"
          >
            <ArrowLeftRight className="h-4 w-4" aria-hidden />
          </Button>
          <div className="flex-1 space-y-1.5">
            <Label className="text-xs text-muted-foreground">To</Label>
            <Select value={to} onValueChange={setTo}>
              <SelectTrigger aria-label="Convert to unit">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {category.units.map((u) => (
                  <SelectItem key={u.id} value={u.id}>
                    {u.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>
      </div>

      {/* result */}
      <div className="rounded-2xl border bg-card px-4 py-3" aria-live="polite">
        {result.ok && result.value !== undefined ? (
          <>
            <p className="text-2xl font-semibold tabular-nums text-primary">
              {result.value}
            </p>
            <p className="mt-0.5 truncate text-xs text-muted-foreground">
              {value || "0"} {unitOf(from)} = {result.value} {unitOf(to)}
            </p>
            {result.reference && (
              <p className="mt-0.5 truncate text-xs text-muted-foreground/70">
                {result.reference}
              </p>
            )}
          </>
        ) : (
          <p className="text-sm text-muted-foreground">
            {result.error || `Choose units to convert ${category.label.toLowerCase()}.`}
          </p>
        )}
      </div>
    </div>
  );
}
