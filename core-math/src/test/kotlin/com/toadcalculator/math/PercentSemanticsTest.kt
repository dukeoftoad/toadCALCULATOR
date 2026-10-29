// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Percent semantics, defined explicitly and tested case by case:
 * - standalone x% = x/100
 * - A + B% = A + A*B/100   (percent of the running total)
 * - A - B% = A - A*B/100
 * - A * B% = A * B/100
 * - A / B% = A / (B/100)
 * - x% in any other position (function argument, exponent, negated, parenthesized)
 *   evaluates as x/100.
 * - '%' followed by an operand is binary modulo instead (10 % 3 = 1).
 */
class PercentSemanticsTest {

    @Test
    fun `standalone percent divides by hundred`() = assertVal(ok("50%"), "0.5")

    @Test
    fun `standalone percent of expression`() = assertVal(ok("(20+30)%"), "0.5")

    @Test
    fun `addition with percent is percent of the total`() = assertVal(ok("200+10%"), "220")

    @Test
    fun `subtraction with percent is percent of the total`() = assertVal(ok("200-10%"), "180")

    @Test
    fun `multiplication by percent`() = assertVal(ok("200*10%"), "20")

    @Test
    fun `division by percent`() = assertVal(ok("200/10%"), "2000")

    @Test
    fun `percent on the left of plus stays plain`() = assertVal(ok("10%+5"), "5.1")

    @Test
    fun `chained additive percents compound`() = assertVal(ok("100+10%+10%"), "121")

    @Test
    fun `double percent is percent of percent`() = assertVal(ok("10%%"), "0.001")

    @Test
    fun `negated percent is plain value`() = assertVal(ok("5*-10%"), "-0.5")

    @Test
    fun `addition of negated percent is plain`() = assertVal(ok("5+-10%"), "4.9")

    @Test
    fun `parenthesized percent is plain in addition`() = assertVal(ok("200+(10%)"), "200.1")

    @Test
    fun `percent as function argument is plain`() = assertFmt("sin(10%)", "0.0998334166468", AngleMode.RAD)

    @Test
    fun `percent in exponent is plain value`() = assertFmt("2^(200%)", "4") // 200% = 2 → 2² = 4

    @Test
    fun `binary modulo still works with spaces`() = assertVal(ok("50 % 7"), "1")

    @Test
    fun `binary modulo after parenthesized sum`() = assertVal(ok("(200+10)%3"), "0")

    @Test
    fun `unary minus then percent then addition`() = assertVal(ok("1+10%"), "1.1")

    @Test
    fun `percent of a decimal total`() = assertVal(ok("0.5+50%"), "0.75")

    @Test
    fun `subtracting percent chains`() = assertVal(ok("100-10%-10%"), "81")
}
