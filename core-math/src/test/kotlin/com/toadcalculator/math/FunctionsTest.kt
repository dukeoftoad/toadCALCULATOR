// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FunctionsTest {

    // ---- trigonometry, DEG ----

    @Test
    fun `sin 90 degrees is exactly one`() = assertFmt("sin(90)", "1")

    @Test
    fun `sin 180 degrees formats as exactly zero`() = assertFmt("sin(180)", "0")

    @Test
    fun `sin 30 degrees is one half`() = assertFmt("sin(30)", "0.5")

    @Test
    fun `sin 0 is zero`() = assertFmt("sin(0)", "0")

    @Test
    fun `cos 0 is one`() = assertFmt("cos(0)", "1")

    @Test
    fun `cos 60 degrees is one half`() = assertFmt("cos(60)", "0.5")

    @Test
    fun `cos 90 degrees formats as exactly zero`() = assertFmt("cos(90)", "0")

    @Test
    fun `tan 45 degrees is one`() = assertFmt("tan(45)", "1")

    @Test
    fun `tan 0 is zero`() = assertFmt("tan(0)", "0")

    @Test
    fun `tan 90 degrees is a domain error`() {
        assertThat(errOf("tan(90)")).isEqualTo(MathError.DomainError)
    }

    @Test
    fun `tan 270 degrees is a domain error`() {
        assertThat(errOf("tan(270)")).isEqualTo(MathError.DomainError)
    }

    @Test
    fun `tan negative 90 degrees is a domain error`() {
        assertThat(errOf("tan(-90)")).isEqualTo(MathError.DomainError)
    }

    // ---- trigonometry, RAD ----

    @Test
    fun `sin of one radian`() = assertFmt("sin(1)", "0.841470984808", AngleMode.RAD)

    @Test
    fun `sin of pi radian formats as exactly zero`() = assertFmt("sin(π)", "0", AngleMode.RAD)

    @Test
    fun `cos of pi over two formats as exactly zero`() = assertFmt("cos(π/2)", "0", AngleMode.RAD)

    @Test
    fun `angle mode changes results`() {
        val deg = NumberFormatter.format(ok("sin(1)", AngleMode.DEG))
        val rad = NumberFormatter.format(ok("sin(1)", AngleMode.RAD))
        assertThat(deg).isEqualTo("0.0174524064373")
        assertThat(rad).isEqualTo("0.841470984808")
    }

    // ---- inverse trigonometry ----

    @Test
    fun `asin one half is 30 degrees`() = assertFmt("asin(0.5)", "30")

    @Test
    fun `asin one is 90 degrees`() = assertFmt("asin(1)", "90")

    @Test
    fun `asin outside domain is a domain error`() {
        assertThat(errOf("asin(1.5)")).isEqualTo(MathError.DomainError)
        assertThat(errOf("acos(-1.0000000001)")).isEqualTo(MathError.DomainError)
    }

    @Test
    fun `acos one is zero`() = assertFmt("acos(1)", "0")

    @Test
    fun `atan one is 45 degrees`() = assertFmt("atan(1)", "45")

    @Test
    fun `atan zero is zero`() = assertFmt("atan(0)", "0")

    // ---- hyperbolics (always radian-domain, angle mode ignored) ----

    @Test
    fun `sinh zero is zero`() = assertFmt("sinh(0)", "0")

    @Test
    fun `cosh zero is one`() = assertFmt("cosh(0)", "1")

    @Test
    fun `tanh one`() = assertFmt("tanh(1)", "0.761594155956")

    @Test
    fun `cosh minus three`() = assertFmt("cosh(-3)", "10.0676619958")

    @Test
    fun `sinh overflow is Overflow`() {
        assertThat(errOf("sinh(1000)")).isEqualTo(MathError.Overflow)
    }

    // ---- logarithms and roots ----

    @Test
    fun `ln of e is one`() = assertFmt("ln(e)", "1")

    @Test
    fun `ln of one is zero`() = assertFmt("ln(1)", "0")

    @Test
    fun `ln of zero is a domain error`() {
        assertThat(errOf("ln(0)")).isEqualTo(MathError.DomainError)
    }

    @Test
    fun `ln of negative is a domain error`() {
        assertThat(errOf("ln(-1)")).isEqualTo(MathError.DomainError)
    }

    @Test
    fun `log base ten`() = assertFmt("log(100)", "2")

    @Test
    fun `log2 of eight`() = assertFmt("log2(8)", "3")

    @Test
    fun `sqrt sixteen is four`() = assertFmt("sqrt(16)", "4")

    @Test
    fun `sqrt two`() = assertFmt("sqrt(2)", "1.41421356237")

    @Test
    fun `sqrt of negative is a domain error`() {
        assertThat(errOf("sqrt(-1)")).isEqualTo(MathError.DomainError)
    }

    @Test
    fun `sqrt zero is zero`() = assertFmt("sqrt(0)", "0")

    @Test
    fun `cbrt of negative eight is minus two`() = assertFmt("cbrt(-8)", "-2")

    @Test
    fun `cbrt of twenty seven is three`() = assertFmt("cbrt(27)", "3")

    // ---- misc ----

    @Test
    fun `abs on bigdecimal is exact`() {
        assertVal(ok("abs(-5)"), "5")
        assertVal(ok("abs(5)"), "5")
    }

    @Test
    fun `exp zero is one`() = assertFmt("exp(0)", "1")

    @Test
    fun `exp one`() = assertFmt("exp(1)", "2.71828182846")

    @Test
    fun `exp overflow is Overflow`() {
        assertThat(errOf("exp(1000)")).isEqualTo(MathError.Overflow)
    }

    @Test
    fun `floor ceiling round on exact decimals`() {
        assertVal(ok("floor(2.7)"), "2")
        assertVal(ok("floor(-2.1)"), "-3")
        assertVal(ok("ceil(2.1)"), "3")
        assertVal(ok("ceil(-2.9)"), "-2")
        assertVal(ok("round(2.5)"), "3")
        assertVal(ok("round(-2.5)"), "-3") // HALF_UP: away from zero
        assertVal(ok("round(2.4)"), "2")
    }

    @Test
    fun `constants have double-exact values`() {
        assertFmt("π", "3.14159265359")
        assertFmt("e", "2.71828182846")
    }
}
