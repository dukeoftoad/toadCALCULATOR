// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ArithmeticTest {

    @Test
    fun `decimal addition is exact`() = assertVal(ok("0.1+0.2"), "0.3")

    @Test
    fun `decimal subtraction is exact`() = assertVal(ok("1.5-0.25"), "1.25")

    @Test
    fun `decimal multiplication is exact`() = assertVal(ok("0.2*0.3"), "0.06")

    @Test
    fun `division is rounded to decimal64`() = assertFmt("1/3", "0.333333333333")

    @Test
    fun `one third times three formats as one`() = assertFmt("(1/3)*3", "1")

    @Test
    fun `division by zero is DivisionByZero`() {
        assertThat(errOf("1/0")).isEqualTo(MathError.DivisionByZero)
    }

    @Test
    fun `zero over zero is Undefined`() {
        assertThat(errOf("0/0")).isEqualTo(MathError.Undefined)
    }

    @Test
    fun `modulo by zero is DivisionByZero`() {
        assertThat(errOf("5%0")).isEqualTo(MathError.DivisionByZero)
    }

    @Test
    fun `zero modulo zero is Undefined`() {
        assertThat(errOf("0%0")).isEqualTo(MathError.Undefined)
    }

    @Test
    fun `integer modulo`() = assertVal(ok("7%3"), "1")

    @Test
    fun `fractional modulo`() = assertVal(ok("7.5%2"), "1.5")

    @Test
    fun `modulo sign follows dividend`() {
        assertVal(ok("-7%3"), "-1")
        assertVal(ok("7 % (-3)"), "1")
    }

    @Test
    fun `large plain integers stay exact`() = assertVal(ok("1000000000000+1"), "1000000000001")

    @Test
    fun `power with positive integral exponent`() = assertVal(ok("10^21"), "1E21")

    @Test
    fun `power beyond the decimal128 bound overflows`() {
        assertThat(errOf("10^6145")).isEqualTo(MathError.Overflow)
        assertThat(errOf("9^9999")).isEqualTo(MathError.Overflow)
    }

    @Test
    fun `power near the bound succeeds`() {
        val v = ok("2^2000")
        // 2^2000 ≈ 1.148e602, comfortably under 1e6144. BigDecimal compare: 1e601 < v < 1e603.
        assertThat(v.compareTo(bd("1E+601")) > 0).isTrue()
        assertThat(v.compareTo(bd("1E+603")) < 0).isTrue()
    }

    @Test
    fun `fractional power uses doubles`() = assertFmt("2^0.5", "1.41421356237")

    @Test
    fun `negative base with fractional exponent is a domain error`() {
        assertThat(errOf("(-2)^0.5")).isEqualTo(MathError.DomainError)
    }

    @Test
    fun `zero to zero is one`() = assertVal(ok("0^0"), "1")

    @Test
    fun `zero to positive power is zero`() = assertVal(ok("0^2"), "0")

    @Test
    fun `zero to negative power is DivisionByZero`() {
        assertThat(errOf("0^-2")).isEqualTo(MathError.DivisionByZero)
    }

    @Test
    fun `zero to fractional power is zero`() = assertVal(ok("0^0.5"), "0")

    @Test
    fun `astronomical literal overflows`() {
        assertThat(errOf("1e999999999")).isEqualTo(MathError.Overflow)
        assertThat(errOf("1e999999999+1")).isEqualTo(MathError.Overflow)
    }

    @Test
    fun `astronomical exponents have defined semantics`() {
        assertThat(errOf("2^999999999999999999999")).isEqualTo(MathError.Overflow)
        assertVal(ok("0.5^999999999999999999999"), "0") // underflow to zero
        assertVal(ok("1^999999999999999999999"), "1")
        assertVal(ok("(-1)^1000000000000000000001"), "-1") // parity: 10^21+1 is odd
    }

    @Test
    fun `unary chains apply left to right`() = assertVal(ok("2*-3"), "-6")

    @Test
    fun `addition of unary minus`() = assertVal(ok("2+-3"), "-1")

    @Test
    fun `deeply nested parens evaluate`() = assertVal(ok("((((((1+1))))))"), "2")
}
