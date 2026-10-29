// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ParserTest {

    // ---- precedence and associativity ----

    @Test
    fun `multiplication binds tighter than addition`() = assertVal(ok("2+3*4"), "14")

    @Test
    fun `left associative addition`() = assertVal(ok("10-3-2"), "5")

    @Test
    fun `left associative division`() = assertVal(ok("100/5/2"), "10")

    @Test
    fun `left associative multiplication`() = assertVal(ok("2*3*4"), "24")

    @Test
    fun `power binds tighter than unary minus so -2^2 is -4`() = assertVal(ok("-2^2"), "-4")

    @Test
    fun `parenthesized base squares to positive`() = assertVal(ok("(-2)^2"), "4")

    @Test
    fun `power is right associative`() = assertVal(ok("2^3^2"), "512")

    @Test
    fun `unary minus folds twice`() = assertVal(ok("--2"), "2")

    @Test
    fun `parenthesized double negation`() = assertVal(ok("-(-2)"), "2")

    @Test
    fun `negative exponent works`() = assertFmt("2^-3", "0.125")

    @Test
    fun `postfix factorial binds tighter than power`() = assertVal(ok("2^3!"), "64")

    @Test
    fun `parenthesized power factorial is 6 factorial`() = assertVal(ok("(2^3)!"), "40320")

    // ---- implicit multiplication ----

    @Test
    fun `number times parenthesis`() = assertVal(ok("2(3+4)"), "14")

    @Test
    fun `parenthesis times parenthesis`() = assertVal(ok("(1+2)(3+4)"), "21")

    @Test
    fun `number times constant`() = assertFmt("2π", "6.28318530718")

    @Test
    fun `number times function call`() = assertFmt("2sin(30)", "1")

    @Test
    fun `number times euler`() = assertFmt("3e", "8.15484548538")

    @Test
    fun `implicit multiplication is left associative`() = assertVal(ok("2(3)(4)"), "24")

    @Test
    fun `power binds tighter than implicit multiplication`() = assertFmt("2π^2", "19.7392088022")

    @Test
    fun `constant followed by number multiplies`() = assertFmt("π2", "6.28318530718")

    @Test
    fun `rparen followed by function name multiplies`() = assertFmt("(1+2)sin(30)", "1.5")

    @Test
    fun `two bare numbers are a syntax error`() {
        val e = errOf("10 5")
        assertThat(e).isInstanceOf(MathError.SyntaxError::class.java)
    }

    // ---- functions and parens ----

    @Test
    fun `function without parenthesis is a syntax error`() {
        val e = errOf("sin 30")
        assertThat(e).isInstanceOf(MathError.SyntaxError::class.java)
        assertThat((e as MathError.SyntaxError).position).isEqualTo(0)
    }

    @Test
    fun `unclosed function paren is unbalanced`() {
        assertThat(errOf("sin(30")).isEqualTo(MathError.UnbalancedParentheses)
    }

    @Test
    fun `stray closing paren is unbalanced`() {
        assertThat(errOf("1+2)")).isEqualTo(MathError.UnbalancedParentheses)
    }

    @Test
    fun `empty parentheses are unbalanced`() {
        assertThat(errOf("()")).isEqualTo(MathError.UnbalancedParentheses)
    }

    @Test
    fun `leading operator is a syntax error`() {
        val e = errOf("*5")
        assertThat(e).isInstanceOf(MathError.SyntaxError::class.java)
        assertThat((e as MathError.SyntaxError).position).isEqualTo(0)
    }

    @Test
    fun `trailing operator is a syntax error at end of input`() {
        val e = errOf("2+")
        assertThat(e).isInstanceOf(MathError.SyntaxError::class.java)
        assertThat((e as MathError.SyntaxError).position).isEqualTo(2)
    }

    @Test
    fun `doubled caret is a syntax error with position`() {
        val e = errOf("2^^3")
        assertThat(e).isInstanceOf(MathError.SyntaxError::class.java)
        assertThat((e as MathError.SyntaxError).position).isEqualTo(2)
    }

    @Test
    fun `empty input is a syntax error at zero`() {
        val e = errOf("")
        assertThat(e).isInstanceOf(MathError.SyntaxError::class.java)
        assertThat((e as MathError.SyntaxError).position).isEqualTo(0)
    }

    @Test
    fun `whitespace only input is a syntax error at zero`() {
        val e = errOf("   ")
        assertThat(e).isInstanceOf(MathError.SyntaxError::class.java)
        assertThat((e as MathError.SyntaxError).position).isEqualTo(0)
    }

    @Test
    fun `case insensitive functions and constants`() {
        assertFmt("SIN(90)", "1")
        assertFmt("SQRT(4)", "2")
        assertFmt("PI", "3.14159265359")
    }
}
