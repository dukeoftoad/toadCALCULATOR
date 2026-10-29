// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LexerTest {

    @Test
    fun `plain integer expression lexes`() {
        assertFmt("1+2", "3")
    }

    @Test
    fun `e followed by digits is an exponent`() {
        assertVal(ok("2e3"), "2000")
    }

    @Test
    fun `e followed by digits with explicit plus is an exponent`() {
        assertVal(ok("1.5e+3"), "1500")
    }

    @Test
    fun `trailing e is Euler constant via implicit multiplication`() {
        assertFmt("2e", "5.43656365692")
    }

    @Test
    fun `explicit e multiplication matches implicit form`() {
        assertFmt("2*e", "5.43656365692")
    }

    @Test
    fun `dot before exponent is normalized`() {
        assertVal(ok("1.e3"), "1000")
    }

    @Test
    fun `leading dot number is accepted`() {
        assertVal(ok(".5"), "0.5")
    }

    @Test
    fun `trailing dot number is accepted`() {
        assertVal(ok("5."), "5")
    }

    @Test
    fun `second dot is a syntax error with position`() {
        val e = errOf("1.2.3")
        assertThat(e).isInstanceOf(MathError.SyntaxError::class.java)
        assertThat((e as MathError.SyntaxError).position).isEqualTo(3)
    }

    @Test
    fun `invalid character reports its position`() {
        val e = errOf("1 @ 2")
        assertThat(e).isInstanceOf(MathError.SyntaxError::class.java)
        assertThat((e as MathError.SyntaxError).position).isEqualTo(2)
    }

    @Test
    fun `unicode operators map to ascii`() {
        assertVal(ok("2×3"), "6")
        assertVal(ok("6÷2"), "3")
        assertVal(ok("5−3"), "2")
    }

    @Test
    fun `pi constant lexes from unicode symbol`() {
        assertFmt("π", "3.14159265359")
    }

    @Test
    fun `percent followed by operand is binary modulo`() {
        assertVal(ok("10%3"), "1")
        assertVal(ok("10 % 3"), "1")
    }

    @Test
    fun `percent followed by operator is postfix percent`() {
        assertVal(ok("10%+5"), "5.1")
    }

    @Test
    fun `percent at end of input is postfix percent`() {
        assertVal(ok("50 %"), "0.5")
    }
}
