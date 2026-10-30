// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2025 toadCALCULATOR contributors
"use client";

import { useCallback, useEffect, useRef, useState, useSyncExternalStore } from "react";
import { todayYMD } from "@/lib/tools/datetime";

const noopSubscribe = () => () => {};

/**
 * True once the client has mounted — guards persisted store hydration.
 * Implemented with useSyncExternalStore: server snapshot `false`,
 * client snapshot `true`, no cascading setState effect.
 */
export function useHydrated(): boolean {
  return useSyncExternalStore(
    noopSubscribe,
    () => true,
    () => false,
  );
}

/** Today's local date as ISO "YYYY-MM-DD", empty on the server snapshot. */
export function useTodayIso(): string {
  return useSyncExternalStore(noopSubscribe, getTodaySnapshot, () => "");
}

function getTodaySnapshot(): string {
  const t = todayYMD();
  return `${t.y}-${String(t.m).padStart(2, "0")}-${String(t.d).padStart(2, "0")}`;
}

/**
 * Long-press gesture for touch AND mouse. After the callback fires, the
 * following click event is swallowed so a long-press never double-triggers.
 */
export function useLongPress(onLongPress: () => void, ms = 550) {
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const firedRef = useRef(false);
  const [pressing, setPressing] = useState(false);

  const start = useCallback(() => {
    firedRef.current = false;
    setPressing(true);
    timerRef.current = setTimeout(() => {
      firedRef.current = true;
      onLongPress();
    }, ms);
  }, [onLongPress, ms]);

  const cancel = useCallback(() => {
    setPressing(false);
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
  }, []);

  /** attach to onClick: returns true if the click should be ignored */
  const consumeClick = useCallback(() => {
    if (firedRef.current) {
      firedRef.current = false;
      return true;
    }
    return false;
  }, []);

  useEffect(() => cancel, [cancel]);

  return {
    handlers: {
      onPointerDown: start,
      onPointerUp: cancel,
      onPointerLeave: cancel,
      onPointerCancel: cancel,
    },
    consumeClick,
    pressing,
  };
}
