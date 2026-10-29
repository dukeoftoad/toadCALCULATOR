// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal

class PlaceholderTest {
    @Test
    fun `placeholder module wires up`() {
        val r = MathResult.Success(BigDecimal("2"))
        assertThat(r).isInstanceOf(MathResult.Success::class.java)
        assertThat((r as MathResult.Success).value.toPlainString()).isEqualTo("2")
    }
}
