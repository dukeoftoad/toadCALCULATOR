// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs

/**
 * Function table for the evaluator. Pure JVM; all double conversions happen
 * here and only here.
 *
 * Conventions (documented in WORKLOG and tests):
 * - sin/cos/tan snap results with |r| < 1e-12 to exactly 0 so sin(180°) = 0.
 * - tan is a DomainError when |tan| > 1e15 (covers 90° in DEG and π/2 in RAD).
 * - DEG/RAD affects sin cos tan asin acos atan only; hyperbolics are always RAD.
 * - round() uses HALF_UP (away from zero at .5) on exact BigDecimal values.
 * - exp/sinh/cosh returning ±Infinity is an Overflow; NaN is a DomainError.
 */
object Functions {

    const val TINY_SNAP = 1e-12
    const val TAN_LIMIT = 1e15

    val NAMES: Set<String> = setOf(
        "sin", "cos", "tan",
        "asin", "acos", "atan",
        "sinh", "cosh", "tanh",
        "ln", "log", "log2",
        "sqrt", "cbrt", "abs",
        "exp", "floor", "ceil", "round",
    )

    fun apply(name: String, x: BigDecimal, angleMode: AngleMode): BigDecimal = when (name) {
        "sin" -> trig({ Math.sin(it) }, x, angleMode)
        "cos" -> trig({ Math.cos(it) }, x, angleMode)
        "tan" -> trigTan(x, angleMode)
        "asin" -> inverseTrig({ Math.asin(it) }, x, angleMode, lower = -1.0, upper = 1.0)
        "acos" -> inverseTrig({ Math.acos(it) }, x, angleMode, lower = -1.0, upper = 1.0)
        "atan" -> inverseTrig({ Math.atan(it) }, x, angleMode, lower = null, upper = null)
        "sinh" -> doubleToBigDecimal(Math.sinh(x.toDouble()), allowInfinity = false)
        "cosh" -> doubleToBigDecimal(Math.cosh(x.toDouble()), allowInfinity = false)
        "tanh" -> doubleToBigDecimal(Math.tanh(x.toDouble()), allowInfinity = true)
        "ln" -> {
            requirePositive(x)
            doubleToBigDecimal(Math.log(x.toDouble()), allowInfinity = false)
        }
        "log" -> {
            requirePositive(x)
            doubleToBigDecimal(Math.log10(x.toDouble()), allowInfinity = false)
        }
        "log2" -> {
            requirePositive(x)
            doubleToBigDecimal(Math.log(x.toDouble()) / Math.log(2.0), allowInfinity = false)
        }
        "sqrt" -> {
            if (x.signum() < 0) throw MathEvalException(MathError.DomainError)
            doubleToBigDecimal(Math.sqrt(x.toDouble()), allowInfinity = false)
        }
        "cbrt" -> doubleToBigDecimal(Math.cbrt(x.toDouble()), allowInfinity = false)
        "abs" -> x.abs()
        "exp" -> doubleToBigDecimal(Math.exp(x.toDouble()), allowInfinity = false)
        "floor" -> x.setScale(0, RoundingMode.FLOOR)
        "ceil" -> x.setScale(0, RoundingMode.CEILING)
        "round" -> x.setScale(0, RoundingMode.HALF_UP)
        else -> throw MathEvalException(MathError.DomainError)
    }

    private fun requirePositive(x: BigDecimal) {
        if (x.signum() <= 0) throw MathEvalException(MathError.DomainError)
    }

    private fun toRadians(x: BigDecimal, angleMode: AngleMode): Double = when (angleMode) {
        AngleMode.DEG -> Math.toRadians(x.toDouble())
        AngleMode.RAD -> x.toDouble()
    }

    private fun fromRadians(y: Double, angleMode: AngleMode): Double = when (angleMode) {
        AngleMode.DEG -> Math.toDegrees(y)
        AngleMode.RAD -> y
    }

    private fun trig(f: (Double) -> Double, x: BigDecimal, angleMode: AngleMode): BigDecimal {
        val r = f(toRadians(x, angleMode))
        val snapped = if (abs(r) < TINY_SNAP) 0.0 else r
        return doubleToBigDecimal(snapped, allowInfinity = true)
    }

    private fun trigTan(x: BigDecimal, angleMode: AngleMode): BigDecimal {
        val r = Math.tan(toRadians(x, angleMode))
        if (abs(r) > TAN_LIMIT) throw MathEvalException(MathError.DomainError)
        val snapped = if (abs(r) < TINY_SNAP) 0.0 else r
        return doubleToBigDecimal(snapped, allowInfinity = true)
    }

    private fun inverseTrig(
        f: (Double) -> Double,
        x: BigDecimal,
        angleMode: AngleMode,
        lower: Double?,
        upper: Double?,
    ): BigDecimal {
        val xd = x.toDouble()
        if (lower != null && upper != null && (xd < lower || xd > upper)) {
            throw MathEvalException(MathError.DomainError)
        }
        val r = fromRadians(f(xd), angleMode)
        return doubleToBigDecimal(r, allowInfinity = false)
    }

    private fun doubleToBigDecimal(d: Double, allowInfinity: Boolean): BigDecimal = when {
        d.isNaN() -> throw MathEvalException(MathError.DomainError)
        d.isInfinite() && !allowInfinity -> throw MathEvalException(MathError.Overflow)
        else -> BigDecimal.valueOf(d)
    }
}
