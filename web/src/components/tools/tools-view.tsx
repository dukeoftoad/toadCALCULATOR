// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2025 toadCALCULATOR contributors
"use client";

import { Ruler, CalendarDays, HeartPulse, CalendarRange } from "lucide-react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { UnitConverter } from "@/components/tools/unit-converter";
import { BmiCalculator } from "@/components/tools/bmi-calculator";
import { AgeCalculator } from "@/components/tools/age-calculator";
import { DateDiffTool } from "@/components/tools/date-diff";

export function ToolsView() {
  return (
    <Tabs defaultValue="converter" className="w-full">
      <TabsList className="grid w-full grid-cols-4">
        <TabsTrigger value="converter" className="gap-1.5">
          <Ruler className="h-4 w-4" aria-hidden />
          <span className="hidden sm:inline">Units</span>
        </TabsTrigger>
        <TabsTrigger value="bmi" className="gap-1.5">
          <HeartPulse className="h-4 w-4" aria-hidden />
          <span className="hidden sm:inline">BMI</span>
        </TabsTrigger>
        <TabsTrigger value="age" className="gap-1.5">
          <CalendarDays className="h-4 w-4" aria-hidden />
          <span className="hidden sm:inline">Age</span>
        </TabsTrigger>
        <TabsTrigger value="datediff" className="gap-1.5">
          <CalendarRange className="h-4 w-4" aria-hidden />
          <span className="hidden sm:inline">Dates</span>
        </TabsTrigger>
      </TabsList>
      <TabsContent value="converter" className="mt-4">
        <UnitConverter />
      </TabsContent>
      <TabsContent value="bmi" className="mt-4">
        <BmiCalculator />
      </TabsContent>
      <TabsContent value="age" className="mt-4">
        <AgeCalculator />
      </TabsContent>
      <TabsContent value="datediff" className="mt-4">
        <DateDiffTool />
      </TabsContent>
    </Tabs>
  );
}
