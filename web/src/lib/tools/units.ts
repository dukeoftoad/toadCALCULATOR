// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2025 toadCALCULATOR contributors
//
// Unit conversion tables — exact international factors as decimal string
// constants (Kotlin-constant style, no JSON assets). 1 GB = 1000 MB;
// binary units (KiB…) are listed separately. Temperature uses offset-aware
// conversion rather than linear factors.

import Decimal from "decimal.js";
import { formatNumber } from "@/lib/math";

export interface UnitDef {
  id: string;
  label: string;
  /** multiplicative factor relative to the category base unit */
  factor: string;
}

export interface UnitCategory {
  id: string;
  label: string;
  base: string;
  units: UnitDef[];
  kind?: "temperature";
}

export const UNIT_CATEGORIES: UnitCategory[] = [
  {
    id: "length",
    label: "Length",
    base: "m",
    units: [
      { id: "mm", label: "Millimeter (mm)", factor: "0.001" },
      { id: "cm", label: "Centimeter (cm)", factor: "0.01" },
      { id: "m", label: "Meter (m)", factor: "1" },
      { id: "km", label: "Kilometer (km)", factor: "1000" },
      { id: "in", label: "Inch (in)", factor: "0.0254" },
      { id: "ft", label: "Foot (ft)", factor: "0.3048" },
      { id: "yd", label: "Yard (yd)", factor: "0.9144" },
      { id: "mi", label: "Mile (mi)", factor: "1609.344" },
      { id: "nmi", label: "Nautical mile (nmi)", factor: "1852" },
    ],
  },
  {
    id: "area",
    label: "Area",
    base: "m²",
    units: [
      { id: "mm2", label: "Square millimeter (mm²)", factor: "0.000001" },
      { id: "cm2", label: "Square centimeter (cm²)", factor: "0.0001" },
      { id: "m2", label: "Square meter (m²)", factor: "1" },
      { id: "ha", label: "Hectare (ha)", factor: "10000" },
      { id: "km2", label: "Square kilometer (km²)", factor: "1000000" },
      { id: "in2", label: "Square inch (in²)", factor: "0.00064516" },
      { id: "ft2", label: "Square foot (ft²)", factor: "0.09290304" },
      { id: "ac", label: "Acre (ac)", factor: "4046.8564224" },
      { id: "mi2", label: "Square mile (mi²)", factor: "2589988.110336" },
    ],
  },
  {
    id: "volume",
    label: "Volume",
    base: "L",
    units: [
      { id: "ml", label: "Milliliter (mL)", factor: "0.001" },
      { id: "l", label: "Liter (L)", factor: "1" },
      { id: "m3", label: "Cubic meter (m³)", factor: "1000" },
      { id: "tsp", label: "Teaspoon (US)", factor: "0.00492892159375" },
      { id: "tbsp", label: "Tablespoon (US)", factor: "0.01478676478125" },
      { id: "floz", label: "Fluid ounce (US)", factor: "0.0295735295625" },
      { id: "cup", label: "Cup (US)", factor: "0.2365882365" },
      { id: "pt", label: "Pint (US)", factor: "0.473176473" },
      { id: "qt", label: "Quart (US)", factor: "0.946352946" },
      { id: "gal", label: "Gallon (US)", factor: "3.785411784" },
    ],
  },
  {
    id: "mass",
    label: "Mass",
    base: "kg",
    units: [
      { id: "mg", label: "Milligram (mg)", factor: "0.000001" },
      { id: "g", label: "Gram (g)", factor: "0.001" },
      { id: "kg", label: "Kilogram (kg)", factor: "1" },
      { id: "t", label: "Metric ton (t)", factor: "1000" },
      { id: "oz", label: "Ounce (oz)", factor: "0.028349523125" },
      { id: "lb", label: "Pound (lb)", factor: "0.45359237" },
      { id: "st", label: "Stone (st)", factor: "6.35029318" },
    ],
  },
  {
    id: "temperature",
    label: "Temperature",
    base: "°C",
    kind: "temperature",
    units: [
      { id: "c", label: "Celsius (°C)", factor: "1" },
      { id: "f", label: "Fahrenheit (°F)", factor: "1" },
      { id: "k", label: "Kelvin (K)", factor: "1" },
      { id: "r", label: "Rankine (°R)", factor: "1" },
    ],
  },
  {
    id: "speed",
    label: "Speed",
    base: "m/s",
    units: [
      { id: "mps", label: "Meter/second (m/s)", factor: "1" },
      { id: "kmh", label: "Kilometer/hour (km/h)", factor: "0.2777777777777778" },
      { id: "mph", label: "Mile/hour (mph)", factor: "0.44704" },
      { id: "kn", label: "Knot (kn)", factor: "0.5144444444444445" },
      { id: "fps", label: "Foot/second (ft/s)", factor: "0.3048" },
    ],
  },
  {
    id: "time",
    label: "Time",
    base: "s",
    units: [
      { id: "ms", label: "Millisecond (ms)", factor: "0.001" },
      { id: "s", label: "Second (s)", factor: "1" },
      { id: "min", label: "Minute (min)", factor: "60" },
      { id: "h", label: "Hour (h)", factor: "3600" },
      { id: "d", label: "Day (d)", factor: "86400" },
      { id: "wk", label: "Week (wk)", factor: "604800" },
      { id: "mo", label: "Month (avg)", factor: "2629746" },
      { id: "yr", label: "Year (avg)", factor: "31556952" },
    ],
  },
  {
    id: "data",
    label: "Data",
    base: "B",
    units: [
      { id: "bit", label: "Bit (b)", factor: "0.125" },
      { id: "byte", label: "Byte (B)", factor: "1" },
      { id: "kb", label: "Kilobyte (KB)", factor: "1000" },
      { id: "mb", label: "Megabyte (MB)", factor: "1000000" },
      { id: "gb", label: "Gigabyte (GB)", factor: "1000000000" },
      { id: "tb", label: "Terabyte (TB)", factor: "1000000000000" },
      { id: "kib", label: "Kibibyte (KiB)", factor: "1024" },
      { id: "mib", label: "Mebibyte (MiB)", factor: "1048576" },
      { id: "gib", label: "Gibibyte (GiB)", factor: "1073741824" },
      { id: "tib", label: "Tebibyte (TiB)", factor: "1099511627776" },
    ],
  },
];

/** Temperature → Celsius (offset-aware, not a linear factor). */
function toCelsius(x: Decimal, unitId: string): Decimal {
  switch (unitId) {
    case "f":
      return x.minus(32).mul(5).div(9);
    case "k":
      return x.minus(273.15);
    case "r":
      return x.minus(491.67).mul(5).div(9);
    default:
      return x;
  }
}

/** Celsius → target temperature unit. */
function fromCelsius(c: Decimal, unitId: string): Decimal {
  switch (unitId) {
    case "f":
      return c.mul(9).div(5).plus(32);
    case "k":
      return c.plus(273.15);
    case "r":
      return c.mul(9).div(5).plus(491.67);
    default:
      return c;
  }
}

export interface ConversionResult {
  ok: boolean;
  value?: string;
  error?: string;
  /** "1 m = 3.28084 ft" style reference line */
  reference?: string;
}

/**
 * Convert `input` (a decimal number) from one unit to another within a
 * category. Results carry ~10 significant digits, like the Android tool.
 */
export function convert(
  categoryId: string,
  fromId: string,
  toId: string,
  input: string,
): ConversionResult {
  const category = UNIT_CATEGORIES.find((c) => c.id === categoryId);
  if (!category) return { ok: false, error: "Unknown category" };
  const from = category.units.find((u) => u.id === fromId);
  const to = category.units.find((u) => u.id === toId);
  if (!from || !to) return { ok: false, error: "Unknown unit" };

  const trimmed = input.trim();
  if (trimmed === "") return { ok: false, error: "" };
  let value: Decimal;
  try {
    value = new Decimal(trimmed);
  } catch {
    return { ok: false, error: "Enter a valid number" };
  }
  if (!value.isFinite()) return { ok: false, error: "Enter a finite number" };

  const format = (d: Decimal) =>
    formatNumber(d, { precision: 10, groupThousands: true });

  if (category.kind === "temperature") {
    if (toId === "k" && toCelsius(value, fromId).plus(273.15).isNegative()) {
      return { ok: false, error: "Below absolute zero" };
    }
    if (toId === "c" && value.minus(273.15).isNegative() && fromId === "k") {
      return { ok: false, error: "Below absolute zero" };
    }
    const c = toCelsius(value, fromId);
    const out = fromCelsius(c, toId);
    return {
      ok: true,
      value: format(out),
      reference: `1 ${unitSymbol(category, fromId)} = ${format(fromCelsius(toCelsius(new Decimal(1), fromId), toId))} ${unitSymbol(category, toId)}`,
    };
  }

  const base = value.mul(from.factor);
  const out = base.div(to.factor);
  const one = new Decimal(1).mul(from.factor).div(to.factor);
  return {
    ok: true,
    value: format(out),
    reference: `1 ${unitSymbol(category, fromId)} = ${format(one)} ${unitSymbol(category, toId)}`,
  };
}

function unitSymbol(category: UnitCategory, unitId: string): string {
  const u = category.units.find((x) => x.id === unitId);
  const label = u?.label ?? "";
  const m = label.match(/\(([^)]+)\)$/);
  return m ? m[1] : label;
}
