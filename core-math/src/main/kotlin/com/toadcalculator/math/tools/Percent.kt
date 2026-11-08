// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math.tools

/**
 * Four common percentage calculations, all pure and offline. [a] and [b] are
 * the two operand slots whose meaning depends on [PercentMode]; see each
 * case for the exact formula. NaN/Infinity inputs propagate to NaN output
 * so the UI can render its "—" placeholder.
 */
enum class PercentMode { OF_TOTAL, PORTION, INCREASE, DECREASE }

object Percent {

    /**
     * - [PercentMode.OF_TOTAL]  — "a is what % of b?" → a / b × 100 (NaN if b = 0)
     * - [PercentMode.PORTION]   — "a% of b"            → a / 100 × b
     * - [PercentMode.INCREASE]  — "b increased by a%"  → b × (1 + a/100)
     * - [PercentMode.DECREASE]  — "b decreased by a%"  → b × (1 − a/100)
     */
    fun compute(mode: PercentMode, a: Double, b: Double): Double = when (mode) {
        PercentMode.OF_TOTAL -> if (b == 0.0) Double.NaN else (a / b) * 100.0
        PercentMode.PORTION -> (a / 100.0) * b
        PercentMode.INCREASE -> b * (1.0 + a / 100.0)
        PercentMode.DECREASE -> b * (1.0 - a / 100.0)
    }
}
