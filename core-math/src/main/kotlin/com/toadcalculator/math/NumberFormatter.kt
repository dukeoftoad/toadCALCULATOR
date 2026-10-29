// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs

/**
 * Display formatter.
 *
 * - Rounds to [precision] significant digits (default 12; app allows 6/8/10/12/15).
 * - Strips trailing zeros; never prints artifacts like 1.0000000000000002.
 * - Switches to scientific notation when |x| ≥ 1e12 or 0 < |x| < 1e-9.
 * - Thousands separators are applied only to the plain (non-scientific)
 *   display string when [grouping] is true; the machine-readable form
 *   (used when loading a result back into the input line) never groups.
 */
object NumberFormatter {

    const val DEFAULT_PRECISION = 12
    val ALLOWED_PRECISIONS = setOf(6, 8, 10, 12, 15)

    private val SCI_UPPER = BigDecimal("1E+12")
    private val SCI_LOWER = BigDecimal("1E-9")

    fun format(
        value: BigDecimal,
        precision: Int = DEFAULT_PRECISION,
        grouping: Boolean = false,
    ): String {
        require(precision in ALLOWED_PRECISIONS) { "Unsupported precision: $precision" }
        if (value.signum() == 0) return "0"

        val rounded = value.round(MathContext(precision))
        val abs = rounded.abs()
        val negative = rounded.signum() < 0

        return if (abs >= SCI_UPPER || abs < SCI_LOWER) {
            formatScientific(abs, negative)
        } else {
            formatPlain(abs, negative, grouping)
        }
    }

    /** Machine-readable (re-parseable) form — no grouping, same rounding rules. */
    fun formatForInput(value: BigDecimal, precision: Int = DEFAULT_PRECISION): String =
        format(value, precision, grouping = false)

    private fun formatPlain(absValue: BigDecimal, negative: Boolean, grouping: Boolean): String {
        var s = absValue.toPlainString()
        val dot = s.indexOf('.')
        if (dot >= 0) {
            var end = s.length
            while (end > dot + 1 && s[end - 1] == '0') end--
            s = if (end == dot + 1) s.substring(0, dot) else s.substring(0, end)
        }
        val body = if (grouping) group(s) else s
        return if (negative) "-$body" else body
    }

    private fun formatScientific(absValue: BigDecimal, negative: Boolean): String {
        var exp = absValue.precision() - absValue.scale() - 1
        var mantissa = absValue.movePointLeft(exp)

        // Rounding at [precision] digits can push the mantissa to exactly 10.
        if (mantissa >= BigDecimal.TEN) {
            mantissa = mantissa.movePointLeft(1)
            exp += 1
        }

        var m = mantissa.stripTrailingZeros().toPlainString()
        val dot = m.indexOf('.')
        if (dot >= 0) {
            var end = m.length
            while (end > dot + 1 && m[end - 1] == '0') end--
            m = if (end == dot + 1) m.substring(0, dot) else m.substring(0, end)
        }
        return (if (negative) "-" else "") + m + "E" + exp
    }

    private fun group(s: String): String {
        val dot = s.indexOf('.')
        val intPart = if (dot >= 0) s.substring(0, dot) else s
        val fracPart = if (dot >= 0) s.substring(dot) else ""
        if (intPart.length <= 3) return s
        val sb = StringBuilder()
        var count = 0
        for (i in intPart.length - 1 downTo 0) {
            sb.append(intPart[i])
            count++
            if (count % 3 == 0 && i > 0) sb.append(',')
        }
        return sb.reverse().toString() + fracPart
    }
}
