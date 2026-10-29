// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math.tools

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BmiTest {

    private fun assertNear(actual: Double, expected: Double, eps: Double = 1e-9) {
        assertThat(actual).isWithin(eps).of(expected)
    }

    @Test
    fun `standard bmi value`() = assertNear(Bmi.bmi(1.80, 75.0), 23.14814814814815)

    @Test
    fun `boundary 18 point 49 is underweight`() = assertThat(Bmi.category(18.49)).isEqualTo(BmiCategory.UNDERWEIGHT)

    @Test
    fun `boundary 18 point 5 is normal`() = assertThat(Bmi.category(18.5)).isEqualTo(BmiCategory.NORMAL)

    @Test
    fun `boundary 24 point 99 is normal`() = assertThat(Bmi.category(24.99)).isEqualTo(BmiCategory.NORMAL)

    @Test
    fun `boundary 25 point 0 is overweight`() = assertThat(Bmi.category(25.0)).isEqualTo(BmiCategory.OVERWEIGHT)

    @Test
    fun `boundary 29 point 99 is overweight`() = assertThat(Bmi.category(29.99)).isEqualTo(BmiCategory.OVERWEIGHT)

    @Test
    fun `boundary 30 point 0 is obese`() = assertThat(Bmi.category(30.0)).isEqualTo(BmiCategory.OBESE)

    @Test
    fun `five foot six and hundred fifty pounds is normal`() {
        val m = Bmi.feetInchesToMeters(5.0, 6.0)
        val kg = Bmi.poundsToKg(150.0)
        val b = Bmi.bmi(m, kg)
        assertThat(Bmi.category(b)).isEqualTo(BmiCategory.NORMAL)
        assertNear(b, 24.21, eps = 0.01)
    }

    @Test
    fun `feet inches conversion exact`() = assertNear(Bmi.feetInchesToMeters(6.0, 0.0), 1.8288)

    @Test
    fun `pounds conversion exact`() = assertNear(Bmi.poundsToKg(1.0), 0.45359237)

    @Test
    fun `healthy range for 1 point 8 meters`() {
        val range = Bmi.healthyWeightRangeKg(1.80)
        assertNear(range.start, 18.5 * 3.24)
        assertNear(range.endInclusive, 24.9 * 3.24)
    }

    @Test
    fun `impossible inputs yield NaN`() {
        assertThat(Bmi.bmi(0.0, 70.0)).isNaN()
        assertThat(Bmi.bmi(-1.7, 70.0)).isNaN()
        assertThat(Bmi.bmi(1.7, 0.0)).isNaN()
        assertThat(Bmi.bmi(1.7, -5.0)).isNaN()
    }

    @Test
    fun `very high bmi still categorizes obese`() = assertThat(Bmi.category(60.0)).isEqualTo(BmiCategory.OBESE)
}
