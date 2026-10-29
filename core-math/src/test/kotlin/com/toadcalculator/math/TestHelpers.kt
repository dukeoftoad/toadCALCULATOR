// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math

import java.math.BigDecimal

/** Shared helpers for engine tests. */

internal fun bd(s: String): BigDecimal = BigDecimal(s)

/** Evaluates and unwraps Success; fails the test with context if the engine returned an error. */
internal fun ok(expr: String, mode: AngleMode = AngleMode.DEG): BigDecimal {
    val r = evaluate(expr, mode)
    if (r !is MathResult.Success) throw AssertionError("expected Success for \"$expr\" but got ${r}")
    return r.value
}

internal fun errOf(expr: String, mode: AngleMode = AngleMode.DEG): MathError {
    val r = evaluate(expr, mode)
    if (r !is MathResult.Failure) throw AssertionError("expected Failure for \"$expr\" but got ${r}")
    return r.error
}

/** Numeric equality ignoring scale (0.30 == 0.3). */
internal fun assertVal(actual: BigDecimal, expected: String) {
    if (actual.stripTrailingZeros().compareTo(bd(expected).stripTrailingZeros()) != 0) {
        throw AssertionError("expected $expected but got ${actual.toPlainString()}")
    }
}

/** Asserts the expression's value through the display formatter (double-derived math). */
internal fun assertFmt(expr: String, expected: String, mode: AngleMode = AngleMode.DEG) {
    val v = ok(expr, mode)
    val f = NumberFormatter.format(v)
    if (f != expected) {
        throw AssertionError("expected \"$expected\" but formatter produced \"$f\" for \"$expr\" (raw=${v.toPlainString()})")
    }
}
