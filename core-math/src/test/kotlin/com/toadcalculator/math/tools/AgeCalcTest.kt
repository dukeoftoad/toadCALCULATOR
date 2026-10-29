// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math.tools

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class AgeCalcTest {

    @Test
    fun `exact birthday anniversary`() {
        val r = AgeCalc.age(LocalDate.of(2000, 1, 1), LocalDate.of(2025, 1, 1))
        assertThat(r.years).isEqualTo(25)
        assertThat(r.months).isEqualTo(0)
        assertThat(r.days).isEqualTo(0)
        // 25 years with 7 leap Februaries (2000, 04, 08, 12, 16, 20, 24).
        assertThat(r.totalDays).isEqualTo(25L * 365 + 7)
    }

    @Test
    fun `month end clamping january 31`() {
        val r = AgeCalc.age(LocalDate.of(2000, 1, 31), LocalDate.of(2000, 3, 1))
        assertThat(r.years).isEqualTo(0)
        assertThat(r.months).isEqualTo(1)
        assertThat(r.days).isEqualTo(1)
    }

    @Test
    fun `feb 29 to feb 28 is eleven months thirty days`() {
        // java.time arithmetic: 2021-02-28 − 2020-02-29 = 364 days = P11M30D.
        val r = AgeCalc.age(LocalDate.of(2020, 2, 29), LocalDate.of(2021, 2, 28))
        assertThat(r.years).isEqualTo(0)
        assertThat(r.months).isEqualTo(11)
        assertThat(r.days).isEqualTo(30)
        assertThat(r.totalDays).isEqualTo(365)
    }

    @Test
    fun `feb 29 birthday celebrates feb 28 in non leap years`() {
        val r = AgeCalc.age(LocalDate.of(2000, 2, 29), LocalDate.of(2025, 1, 10))
        assertThat(r.nextBirthdayDate).isEqualTo(LocalDate.of(2025, 2, 28))
        assertThat(r.daysUntilNextBirthday).isEqualTo(49)
    }

    @Test
    fun `feb 29 birthday celebrates feb 29 in leap years`() {
        val r = AgeCalc.age(LocalDate.of(2000, 2, 29), LocalDate.of(2024, 1, 10))
        assertThat(r.nextBirthdayDate).isEqualTo(LocalDate.of(2024, 2, 29))
        assertThat(r.daysUntilNextBirthday).isEqualTo(50)
    }

    @Test
    fun `birthday today rolls to next year`() {
        val r = AgeCalc.age(LocalDate.of(1990, 5, 15), LocalDate.of(2025, 5, 15))
        assertThat(r.years).isEqualTo(35)
        assertThat(r.nextBirthdayDate).isEqualTo(LocalDate.of(2026, 5, 15))
        assertThat(r.daysUntilNextBirthday).isEqualTo(365)
    }

    @Test
    fun `same day age is zero with a full year to go`() {
        val r = AgeCalc.age(LocalDate.of(2001, 1, 1), LocalDate.of(2001, 1, 1))
        assertThat(r.years).isEqualTo(0)
        assertThat(r.months).isEqualTo(0)
        assertThat(r.days).isEqualTo(0)
        assertThat(r.totalDays).isEqualTo(0)
        assertThat(r.daysUntilNextBirthday).isEqualTo(365)
    }

    @Test
    fun `total days matches a known span`() {
        val r = AgeCalc.age(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1))
        assertThat(r.totalDays).isEqualTo(366) // 2024 is a leap year
        assertThat(r.years).isEqualTo(1)
    }

    @Test
    fun `asOf before birth is rejected`() {
        try {
            AgeCalc.age(LocalDate.of(2000, 1, 1), LocalDate.of(1999, 1, 1))
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // ok
        }
    }
}
