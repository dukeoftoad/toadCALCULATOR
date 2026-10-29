// SPDX-License-Identifier: GPL-3.0-or-later
//
// Placeholder engine types. Replaced by the real expression engine in Phase 1.

package com.toadcalculator.math

import java.math.BigDecimal

/** The angle mode used by trigonometric functions. */
enum class AngleMode { DEG, RAD }

/** Result of evaluating an expression. */
sealed class MathResult {
    /** Evaluation produced a value. */
    data class Success(val value: BigDecimal) : MathResult()

    /** Evaluation produced a typed error. */
    data class Failure(val marker: String) : MathResult()
}
