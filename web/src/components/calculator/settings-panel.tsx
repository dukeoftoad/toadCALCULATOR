// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2025 toadCALCULATOR contributors
"use client";

import { useTheme } from "next-themes";
import { Leaf, ShieldCheck, Trash2, WifiOff } from "lucide-react";
import { useCalcStore, type Precision } from "@/store/calculator";
import { useHydrated } from "@/hooks/use-calc-hooks";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { Button } from "@/components/ui/button";
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

function Row({ label, description, children }: { label: string; description?: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-4 py-3">
      <div className="space-y-0.5">
        <Label className="text-sm font-medium">{label}</Label>
        {description && <p className="text-xs text-muted-foreground">{description}</p>}
      </div>
      <div className="shrink-0">{children}</div>
    </div>
  );
}

export function SettingsPanel() {
  const hydrated = useHydrated();
  const { theme, setTheme } = useTheme();
  const angleMode = useCalcStore((s) => s.angleMode);
  const setAngleMode = useCalcStore((s) => s.setAngleMode);
  const precision = useCalcStore((s) => s.precision);
  const setPrecision = useCalcStore((s) => s.setPrecision);
  const groupThousands = useCalcStore((s) => s.groupThousands);
  const setGroupThousands = useCalcStore((s) => s.setGroupThousands);
  const haptics = useCalcStore((s) => s.haptics);
  const setHaptics = useCalcStore((s) => s.setHaptics);
  const history = useCalcStore((s) => s.history);
  const clearHistory = useCalcStore((s) => s.clearHistory);

  return (
    <div className="flex flex-col gap-4">
      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-base">Appearance</CardTitle>
          <CardDescription>Theme applies instantly and persists on this device.</CardDescription>
        </CardHeader>
        <CardContent className="divide-y">
          <Row label="Theme" description="System follows your device setting">
            <Select value={theme ?? "system"} onValueChange={setTheme}>
              <SelectTrigger className="w-32" aria-label="Theme">
                <SelectValue placeholder="Theme" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="system">System</SelectItem>
                <SelectItem value="light">Light</SelectItem>
                <SelectItem value="dark">Dark</SelectItem>
              </SelectContent>
            </Select>
          </Row>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-base">Calculator</CardTitle>
          <CardDescription>
            Precision changes reformat results immediately — nothing is recomputed.
          </CardDescription>
        </CardHeader>
        <CardContent className="divide-y">
          <Row label="Angle mode" description="Used by trigonometric functions">
            <Select
              value={hydrated ? angleMode : undefined}
              onValueChange={(v) => setAngleMode(v as "deg" | "rad")}
            >
              <SelectTrigger className="w-32" aria-label="Angle mode">
                <SelectValue placeholder="Angle" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="deg">Degrees</SelectItem>
                <SelectItem value="rad">Radians</SelectItem>
              </SelectContent>
            </Select>
          </Row>
          <Row label="Precision" description="Significant digits shown">
            <Select
              value={hydrated ? String(precision) : undefined}
              onValueChange={(v) => setPrecision(Number(v) as Precision)}
            >
              <SelectTrigger className="w-32" aria-label="Precision">
                <SelectValue placeholder="Digits" />
              </SelectTrigger>
              <SelectContent>
                {[6, 8, 10, 12, 15].map((p) => (
                  <SelectItem key={p} value={String(p)}>
                    {p} digits
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </Row>
          <Row label="Thousands separator" description="Display only — never reparsed">
            <Switch
              checked={hydrated && groupThousands}
              onCheckedChange={setGroupThousands}
              aria-label="Thousands separator"
            />
          </Row>
          <Row label="Haptic feedback" description="Vibrate on key press where supported">
            <Switch
              checked={hydrated && haptics}
              onCheckedChange={setHaptics}
              aria-label="Haptic feedback"
            />
          </Row>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-base">Data</CardTitle>
          <CardDescription>Everything lives on this device. Nothing is uploaded.</CardDescription>
        </CardHeader>
        <CardContent>
          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button
                variant="outline"
                className="gap-2 border-destructive/40 text-destructive hover:bg-destructive/10 hover:text-destructive"
                disabled={!hydrated || history.length === 0}
              >
                <Trash2 className="h-4 w-4" aria-hidden /> Clear calculation history
                {hydrated && history.length > 0 ? ` (${history.length})` : ""}
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
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="flex items-center gap-2 text-base">
            <Leaf className="h-4 w-4 text-primary" aria-hidden /> About toadCALCULATOR
          </CardTitle>
          <CardDescription>Version 1.0.0 · Web edition</CardDescription>
        </CardHeader>
        <CardContent className="space-y-3 text-sm text-muted-foreground">
          <p>
            A personal calculator that respects you: fully offline, no ads, no
            tracking, no special permissions — on every platform it runs.
          </p>
          <div className="flex flex-wrap gap-2">
            <span className="inline-flex items-center gap-1.5 rounded-full bg-primary/10 px-3 py-1 text-xs font-medium text-primary">
              <WifiOff className="h-3.5 w-3.5" aria-hidden /> Works offline
            </span>
            <span className="inline-flex items-center gap-1.5 rounded-full bg-primary/10 px-3 py-1 text-xs font-medium text-primary">
              <ShieldCheck className="h-3.5 w-3.5" aria-hidden /> No ads · No tracking
            </span>
          </div>
          <Separator />
          <p className="text-xs leading-relaxed">
            Copyright © 2025 toadCALCULATOR contributors. This program is free
            software: you can redistribute it and/or modify it under the terms of
            the <span className="font-medium text-foreground">GNU General Public License as published by the Free Software Foundation, version 3 or later</span>{" "}
            (GPL-3.0-or-later). There is NO WARRANTY, to the extent permitted by
            law. See the LICENSE file in the source repository for the full text.
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
