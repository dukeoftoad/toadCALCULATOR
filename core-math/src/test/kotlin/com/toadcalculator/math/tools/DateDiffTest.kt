// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math.tools

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class DateDiffTest {

    @Test
    fun `same day is zero exclusive and one inclusive`() {
        val d = LocalDate.of(2025, 6, 15)
        val exclusive = DateDiff.difference(d, d, inclusive = false)
        val inclusive = DateDiff.difference(d, d, inclusive = true)
        assertThat(exclusive.totalDays).isEqualTo(0)
        assertThat(inclusive.totalDays).isEqualTo(1)
    }

    @Test
    fun `leap february spans two exclusive days`() {
        val r = DateDiff.difference(LocalDate.of(2024, 2, 28), LocalDate.of(2024, 3, 1), inclusive = false)
        assertThat(r.totalDays).isEqualTo(2)
        assertThat(DateDiff.difference(LocalDate.of(2024, 2, 28), LocalDate.of(2024, 3, 1), inclusive = true).totalDays)
            .isEqualTo(3)
    }

    @Test
    fun `non leap february spans one exclusive day`() {
        val r = DateDiff.difference(LocalDate.of(2023, 2, 28), LocalDate.of(2023, 3, 1), inclusive = false)
        assertThat(r.totalDays).isEqualTo(1)
    }

    @Test
    fun `one week splits into weeks and remainder`() {
        val r = DateDiff.difference(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 8), inclusive = false)
        assertThat(r.totalDays).isEqualTo(7)
        assertThat(r.weeks).isEqualTo(1)
        assertThat(r.remainderDays).isEqualTo(0)
    }

    @Test
    fun `weeks and remainder days`() {
        val r = DateDiff.difference(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 10), inclusive = false)
        assertThat(r.totalDays).isEqualTo(9)
        assertThat(r.weeks).isEqualTo(1)
        assertThat(r.remainderDays).isEqualTo(2)
    }

    @Test
    fun `years months days decomposition`() {
        val r = DateDiff.difference(LocalDate.of(2020, 1, 15), LocalDate.of(2021, 3, 20), inclusive = false)
        assertThat(r.years).isEqualTo(1)
        assertThat(r.months).isEqualTo(2)
        assertThat(r.days).isEqualTo(5)
    }

    @Test
    fun `inclusive adds one day to the decomposition`() {
        val r = DateDiff.difference(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 1), inclusive = true)
        assertThat(r.totalDays).isEqualTo(1)
        assertThat(r.weeks).isEqualTo(0)
        assertThat(r.remainderDays).isEqualTo(1)
    }

    @Test
    fun `reversed range is rejected`() {
        try {
            DateDiff.difference(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 1, 1), inclusive = false)
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // ok
        }
    }
}
