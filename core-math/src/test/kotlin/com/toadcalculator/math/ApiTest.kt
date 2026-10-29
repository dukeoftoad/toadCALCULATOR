// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

/** Public API surface and architectural guarantees of the engine module. */
class ApiTest {

    @Test
    fun `default angle mode is DEG`() {
        assertFmt("sin(90)", "1")
    }

    @Test
    fun `entry point returns Success wrapper`() {
        val r = evaluate("2+2")
        assertThat(r).isInstanceOf(MathResult.Success::class.java)
        assertThat((r as MathResult.Success).value.toPlainString()).isEqualTo("4")
    }

    @Test
    fun `entry point returns Failure wrapper with typed error`() {
        val r = evaluate("1/0")
        assertThat(r).isInstanceOf(MathResult.Failure::class.java)
        assertThat((r as MathResult.Failure).error).isEqualTo(MathError.DivisionByZero)
    }

    @Test
    fun `syntax error carries a position`() {
        val r = evaluate("1+α")
        assertThat(r).isInstanceOf(MathResult.Failure::class.java)
        val err = (r as MathResult.Failure).error
        assertThat(err).isInstanceOf(MathError.SyntaxError::class.java)
        assertThat((err as MathError.SyntaxError).position).isEqualTo(2)
    }

    @Test
    fun `all six error types are reachable`() {
        assertThat(errOf("1/0")).isEqualTo(MathError.DivisionByZero)
        assertThat(errOf("0/0")).isEqualTo(MathError.Undefined)
        assertThat(errOf("sqrt(-1)")).isEqualTo(MathError.DomainError)
        assertThat(errOf("10^6145")).isEqualTo(MathError.Overflow)
        assertThat(errOf("1+*2")).isInstanceOf(MathError.SyntaxError::class.java)
        assertThat(errOf("(1+2")).isEqualTo(MathError.UnbalancedParentheses)
    }

    @Test
    fun `formatter rejects unsupported precision`() {
        assertThrows(IllegalArgumentException::class.java) {
            NumberFormatter.format(java.math.BigDecimal.ONE, precision = 7)
        }
    }
}
