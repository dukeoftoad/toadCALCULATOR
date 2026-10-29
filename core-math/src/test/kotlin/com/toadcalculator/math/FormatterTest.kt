// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal

class FormatterTest {

    private fun f(v: String, precision: Int = 12, grouping: Boolean = false) =
        NumberFormatter.format(BigDecimal(v), precision, grouping)

    @Test
    fun `zero formats as zero`() {
        assertThat(f("0")).isEqualTo("0")
        assertThat(f("-0.0")).isEqualTo("0")
        assertThat(f("0.000")).isEqualTo("0")
    }

    @Test
    fun `trailing zeros are stripped`() {
        assertThat(f("0.300000000000")).isEqualTo("0.3")
        assertThat(f("2.500")).isEqualTo("2.5")
    }

    @Test
    fun `never prints floating artifacts`() {
        assertThat(f("1.0000000000000002")).isEqualTo("1")
        assertThat(f("0.9999999999999999")).isEqualTo("1")
    }

    @Test
    fun `rounds to twelve significant digits by default`() {
        assertThat(f("3.141592653589793238")).isEqualTo("3.14159265359")
        assertThat(f("123456789012.3456")).isEqualTo("123456789012")
    }

    @Test
    fun `scientific notation at or above one trillion`() {
        assertThat(f("1000000000000")).isEqualTo("1E12")
        assertThat(f("999999999999")).isEqualTo("999999999999")
        assertThat(f("12345678901234567890")).isEqualTo("1.23456789012E19")
    }

    @Test
    fun `scientific notation below one billionth`() {
        assertThat(f("0.000000001")).isEqualTo("0.000000001")
        assertThat(f("0.0000000009")).isEqualTo("9E-10")
        assertThat(f("1.5E-10")).isEqualTo("1.5E-10")
    }

    @Test
    fun `negative scientific notation keeps sign`() {
        assertThat(f("-0.000000000123456")).isEqualTo("-1.23456E-10")
        assertThat(f("-2000000000000")).isEqualTo("-2E12")
    }

    @Test
    fun `mantissa rounding bumps the exponent`() {
        // 999999999999.9999999 rounds at 12 sig digits to 1.00000000000E12.
        assertThat(f("999999999999.9999999")).isEqualTo("1E12")
    }

    @Test
    fun `thousand separators only on display form`() {
        assertThat(f("1234567.891", grouping = true)).isEqualTo("1,234,567.891")
        assertThat(f("1234567.891")).isEqualTo("1234567.891")
        assertThat(f("999999999999", grouping = true)).isEqualTo("999,999,999,999")
        assertThat(f("123.456", grouping = true)).isEqualTo("123.456")
        assertThat(f("-1234.5", grouping = true)).isEqualTo("-1,234.5")
        assertThat(f("1E12", grouping = true)).isEqualTo("1E12") // scientific never groups
    }

    @Test
    fun `precision is configurable`() {
        assertThat(f("3.14159265358979", precision = 6)).isEqualTo("3.14159")
        assertThat(f("3.14159265358979", precision = 8)).isEqualTo("3.1415927")
        assertThat(f("3.14159265358979", precision = 10)).isEqualTo("3.141592654")
        assertThat(f("3.14159265358979", precision = 15)).isEqualTo("3.14159265358979")
    }

    @Test
    fun `precision affects scientific mantissa`() {
        assertThat(f("12345678901234567890", precision = 6)).isEqualTo("1.23457E19")
    }

    @Test
    fun `input form is re-parseable`() {
        val v = ok("1/3", AngleMode.DEG)
        val s = NumberFormatter.formatForInput(v)
        // Feeding the formatted result back into the engine must not crash.
        assertThat(evaluate(s)).isInstanceOf(MathResult.Success::class.java)
    }

    @Test
    fun `rejected precision throws`() {
        try {
            f("1", precision = 7)
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // ok
        }
    }
}
