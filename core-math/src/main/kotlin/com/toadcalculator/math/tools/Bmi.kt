// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math.tools

import kotlin.math.pow

/** WHO BMI categories (four principal classes). */
enum class BmiCategory { UNDERWEIGHT, NORMAL, OVERWEIGHT, OBESE }

object Bmi {

    /**
     * BMI = kg / m². Returns NaN for impossible inputs (height ≤ 0, weight ≤ 0);
     * callers decide how to render it. Pure function; boundary-tested.
     */
    fun bmi(heightMeters: Double, weightKg: Double): Double {
        if (heightMeters <= 0.0 || weightKg <= 0.0) return Double.NaN
        return weightKg / (heightMeters * heightMeters)
    }

    /** WHO thresholds: <18.5 under, 18.5–24.99 normal, 25–29.99 over, ≥30 obese. */
    fun category(bmiValue: Double): BmiCategory = when {
        bmiValue.isNaN() -> BmiCategory.UNDERWEIGHT // treated as "no data" by the UI
        bmiValue < 18.5 -> BmiCategory.UNDERWEIGHT
        bmiValue < 25.0 -> BmiCategory.NORMAL
        bmiValue < 30.0 -> BmiCategory.OVERWEIGHT
        else -> BmiCategory.OBESE
    }

    /** Healthy weight span for a height: 18.5·m² … 24.9·m² (display convention). */
    fun healthyWeightRangeKg(heightMeters: Double): ClosedFloatingPointRange<Double> {
        val lo = 18.5 * heightMeters.pow(2)
        val hi = 24.9 * heightMeters.pow(2)
        return lo..hi
    }

    fun feetInchesToMeters(feet: Double, inches: Double): Double =
        (feet * 12.0 + inches) * 0.0254

    fun poundsToKg(pounds: Double): Double = pounds * 0.45359237
}
