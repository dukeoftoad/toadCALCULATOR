// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2025 toadCALCULATOR contributors
//
// BMI calculation with WHO classification boundaries (18.5 / 25 / 30) and a
// healthy weight range of 18.5·m² – 24.9·m², mirroring the Android tool.

export type BmiCategory = "Underweight" | "Normal weight" | "Overweight" | "Obese";

export interface BmiOutcome {
  ok: boolean;
  message?: string;
  bmi?: number;
  category?: BmiCategory;
  /** healthy range in the user's weight unit */
  rangeLow?: number;
  rangeHigh?: number;
}

export function bmiCategory(bmi: number): BmiCategory {
  if (bmi < 18.5) return "Underweight";
  if (bmi < 25) return "Normal weight";
  if (bmi < 30) return "Overweight";
  return "Obese";
}

export const CATEGORY_TONE: Record<BmiCategory, string> = {
  Underweight: "bg-amber-500/15 text-amber-600 dark:text-amber-400",
  "Normal weight": "bg-emerald-500/15 text-emerald-600 dark:text-emerald-400",
  Overweight: "bg-orange-500/15 text-orange-600 dark:text-orange-400",
  Obese: "bg-red-500/15 text-red-600 dark:text-red-400",
};

/**
 * @param heightM  height in meters (> 0)
 * @param weightKg weight in kilograms (> 0)
 * @param weightUnit "kg" | "lb" — only controls the healthy-range output unit
 */
export function computeBmi(
  heightM: number | null,
  weightKg: number | null,
  weightUnit: "kg" | "lb",
): BmiOutcome {
  if (heightM === null || weightKg === null || Number.isNaN(heightM) || Number.isNaN(weightKg)) {
    return { ok: false, message: "Enter your height and weight to see your BMI." };
  }
  if (heightM <= 0) return { ok: false, message: "Height must be greater than zero." };
  if (weightKg <= 0) return { ok: false, message: "Weight must be greater than zero." };

  const bmi = weightKg / (heightM * heightM);
  if (!Number.isFinite(bmi)) {
    return { ok: false, message: "Those values are out of range." };
  }
  const lowKg = 18.5 * heightM * heightM;
  const highKg = 24.9 * heightM * heightM;
  const toUnit = weightUnit === "kg" ? 1 : 1 / 0.45359237;

  return {
    ok: true,
    bmi: Math.round(bmi * 10) / 10,
    category: bmiCategory(bmi),
    rangeLow: Math.round(lowKg * toUnit * 10) / 10,
    rangeHigh: Math.round(highKg * toUnit * 10) / 10,
  };
}
