// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2025 toadCALCULATOR contributors
//
// Calendar math (java.time-style, UTC-safe): Y/M/D breakdown with month-end
// borrowing, total day counts, next-birthday countdown (Feb 29 → Mar 1 in
// non-leap years) and inclusive/exclusive date differences.

export interface YMD {
  y: number;
  m: number; // 1-12
  d: number;
}

export function isLeap(y: number): boolean {
  return (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0;
}

export function daysInMonth(y: number, m: number): number {
  switch (m) {
    case 1:
    case 3:
    case 5:
    case 7:
    case 8:
    case 10:
    case 12:
      return 31;
    case 4:
    case 6:
    case 9:
    case 11:
      return 30;
    case 2:
      return isLeap(y) ? 29 : 28;
    default:
      return 30;
  }
}

/** Parse "YYYY-MM-DD" (the native date input format) → YMD | null. */
export function parseYMD(s: string): YMD | null {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(s);
  if (!m) return null;
  const y = Number(m[1]);
  const mo = Number(m[2]);
  const d = Number(m[3]);
  if (mo < 1 || mo > 12 || d < 1 || d > daysInMonth(y, mo)) return null;
  return { y, m: mo, d };
}

/** Exact day number since 1970-01-01 (UTC calendar, no DST drift). */
export function dayNumber(ymd: YMD): number {
  return Math.floor(
    Date.UTC(ymd.y, ymd.m - 1, ymd.d) / 86400000,
  );
}

export function todayYMD(): YMD {
  const now = new Date();
  return { y: now.getFullYear(), m: now.getMonth() + 1, d: now.getDate() };
}

/**
 * Calendar-aware a → b difference: years, months, days with borrow from the
 * actual lengths of the months involved (same algorithm as java.time
 * Period.between).
 */
export function diffYMD(a: YMD, b: YMD): { years: number; months: number; days: number } {
  let years = b.y - a.y;
  let months = b.m - a.m;
  let days = b.d - a.d;
  if (days < 0) {
    months -= 1;
    const pm = b.m === 1 ? 12 : b.m - 1;
    const py = b.m === 1 ? b.y - 1 : b.y;
    days += daysInMonth(py, pm);
  }
  if (months < 0) {
    months += 12;
    years -= 1;
  }
  return { years, months, days };
}

export interface AgeOutcome {
  ok: boolean;
  message?: string;
  years?: number;
  months?: number;
  days?: number;
  totalDays?: number;
  totalMonths?: number;
  totalWeeks?: number;
}

/** Age between birth and asOf. */
export function ageBreakdown(birth: YMD, asOf: YMD): AgeOutcome {
  if (dayNumber(birth) > dayNumber(asOf)) {
    return { ok: false, message: "The birth date must be on or before the reference date." };
  }
  const { years, months, days } = diffYMD(birth, asOf);
  const totalDays = dayNumber(asOf) - dayNumber(birth);
  const totalMonths = years * 12 + months;
  return {
    ok: true,
    years,
    months,
    days,
    totalDays,
    totalMonths,
    totalWeeks: Math.floor(totalDays / 7),
  };
}

export interface NextBirthday {
  isToday: boolean;
  daysUntil: number;
  date: YMD;
}

/**
 * Next occurrence of the birth month/day on or after `today`.
 * Feb 29 birthdays celebrate Mar 1 in non-leap years.
 */
export function nextBirthday(birth: YMD, today: YMD): NextBirthday {
  const clamp = (y: number): YMD => {
    if (birth.m === 2 && birth.d === 29 && !isLeap(y)) return { y, m: 3, d: 1 };
    return { y, m: birth.m, d: birth.d };
  };

  let cand = clamp(today.y);
  let cmp = dayNumber(cand) - dayNumber(today);
  if (cmp < 0) {
    cand = clamp(today.y + 1);
    cmp = dayNumber(cand) - dayNumber(today);
  }
  return { isToday: cmp === 0, daysUntil: cmp, date: cand };
}

export interface DateDiffOutcome {
  ok: boolean;
  message?: string;
  totalDays?: number;
  weeks?: number;
  weekRemainder?: number;
  months?: number;
  monthRemainder?: number;
  years?: number;
  monthsOfYearSpan?: number;
  daysOfYearSpan?: number;
}

/** Difference between start and end; `inclusive` counts the end date too. */
export function dateDiff(
  start: YMD,
  end: YMD,
  inclusive: boolean,
): DateDiffOutcome {
  if (dayNumber(start) > dayNumber(end)) {
    return { ok: false, message: "The end date must be on or after the start date." };
  }
  const adjEnd: YMD = inclusive ? addDays(end, 1) : end;
  const totalDays = dayNumber(adjEnd) - dayNumber(start);
  const { years, months, days } = diffYMD(start, adjEnd);
  const totalMonths = years * 12 + months;
  return {
    ok: true,
    totalDays,
    weeks: Math.floor(totalDays / 7),
    weekRemainder: totalDays % 7,
    months: totalMonths,
    monthRemainder: days,
    years,
    monthsOfYearSpan: months,
    daysOfYearSpan: days,
  };
}

function addDays(ymd: YMD, n: number): YMD {
  const dt = new Date(Date.UTC(ymd.y, ymd.m - 1, ymd.d + n));
  return { y: dt.getUTCFullYear(), m: dt.getUTCMonth() + 1, d: dt.getUTCDate() };
}
