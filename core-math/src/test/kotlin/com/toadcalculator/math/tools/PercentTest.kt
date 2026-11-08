// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math.tools

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PercentTest {

    @Test
    fun `of_total computes percentage of a whole`() {
        assertThat(Percent.compute(PercentMode.OF_TOTAL, 25.0, 200.0)).isEqualTo(12.5)
    }

    @Test
    fun `of_total is NaN when whole is zero`() {
        assertThat(Percent.compute(PercentMode.OF_TOTAL, 5.0, 0.0).isNaN()).isTrue()
    }

    @Test
    fun `portion computes a percent of a value`() {
        // 15% of 200 = 30
        assertThat(Percent.compute(PercentMode.PORTION, 15.0, 200.0)).isEqualTo(30.0)
    }

    @Test
    fun `increase adds a percent to a value`() {
        // 10% increase on 200 = 220
        assertThat(Percent.compute(PercentMode.INCREASE, 10.0, 200.0)).isEqualTo(220.0)
    }

    @Test
    fun `decrease subtracts a percent from a value`() {
        // 25% decrease on 200 = 150
        assertThat(Percent.compute(PercentMode.DECREASE, 25.0, 200.0)).isEqualTo(150.0)
    }

    @Test
    fun `decrease can go_negative`() {
        // 200% decrease on 50 = -50
        assertThat(Percent.compute(PercentMode.DECREASE, 200.0, 50.0)).isEqualTo(-50.0)
    }

    @Test
    fun `nan_operands_propagate`() {
        assertThat(Percent.compute(PercentMode.PORTION, Double.NaN, 100.0).isNaN()).isTrue()
    }
}
