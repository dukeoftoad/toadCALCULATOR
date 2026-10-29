// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal

class FactorialTest {

    @Test
    fun `zero factorial is one`() = assertVal(ok("0!"), "1")

    @Test
    fun `five factorial is 120`() = assertVal(ok("5!"), "120")

    @Test
    fun `ten factorial`() = assertVal(ok("10!"), "3628800")

    @Test
    fun `one seventy factorial formats in scientific notation`() {
        assertFmt("170!", "7.25741561531E306")
    }

    @Test
    fun `trailing zero scale factorial is still integral`() = assertVal(ok("5.0!"), "120")

    @Test
    fun `negative factorial is a domain error`() {
        assertThat(errOf("(-1)!")).isEqualTo(MathError.DomainError)
    }

    @Test
    fun `fractional factorial is a domain error`() {
        assertThat(errOf("1.5!")).isEqualTo(MathError.DomainError)
    }

    @Test
    fun `huge factorial overflows`() {
        assertThat(errOf("2500!")).isEqualTo(MathError.Overflow)
    }

    @Test
    fun `astronomical factorial argument overflows fast`() {
        // Must hit the magnitude guard, not iterate billions of times.
        val start = System.nanoTime()
        assertThat(errOf("2000000000!")).isEqualTo(MathError.Overflow)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertThat(elapsedMs).isLessThan(5000)
    }

    @Test
    fun `double factorial chains postfix`() {
        val r = ok("5!!")
        // (5!)! = 120! ≈ 6.69e198
        assertThat(r.compareTo(BigDecimal("1e198")) > 0).isTrue()
        assertThat(r.compareTo(BigDecimal("1e199")) < 0).isTrue()
    }
}
