// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator

import com.google.common.truth.Truth.assertThat
import com.toadcalculator.ui.tools.formatFixed
import com.toadcalculator.ui.tools.formatToolNumber
import org.junit.Test

/**
 * F14: the unified tool-result formatter's contract. These were previously
 * untestable — the formatter lived inside a screen file with the same logic
 * duplicated ad hoc in BMI. Now one implementation, directly pinned.
 */
class ToolsFormatTest {

    // formatToolNumber: ~10 significant digits, trimmed, scientific outside 1e-9…1e12

    @Test
    fun `nan renders as em dash`() {
        assertThat(formatToolNumber(Double.NaN)).isEqualTo("—")
    }

    @Test
    fun `infinity renders as em dash`() {
        assertThat(formatToolNumber(Double.POSITIVE_INFINITY)).isEqualTo("—")
        assertThat(formatToolNumber(Double.NEGATIVE_INFINITY)).isEqualTo("—")
    }

    @Test
    fun `plain value keeps significant digits`() {
        assertThat(formatToolNumber(3.280839895)).isEqualTo("3.280839895")
    }

    @Test
    fun `rounding to 10 significant digits`() {
        assertThat(formatToolNumber(1.0 / 3.0)).isEqualTo("0.3333333333")
    }

    @Test
    fun `trailing zeros are stripped`() {
        assertThat(formatToolNumber(2.5)).isEqualTo("2.5")
        assertThat(formatToolNumber(100.0)).isEqualTo("100")
    }

    @Test
    fun `zero stays plain`() {
        assertThat(formatToolNumber(0.0)).isEqualTo("0")
    }

    @Test
    fun `huge values go scientific with six decimals`() {
        assertThat(formatToolNumber(2.0e15)).isEqualTo("2.000000E+15")
    }

    @Test
    fun `tiny values go scientific with six decimals`() {
        assertThat(formatToolNumber(1.0e-12)).isEqualTo("1.000000E-12")
    }

    // formatFixed: always exactly N decimals, Locale.ROOT

    @Test
    fun `fixed decimals round half-even via formatter`() {
        assertThat(formatFixed(22.85714285, 2)).isEqualTo("22.86")
    }

    @Test
    fun `fixed decimals pad to exact width`() {
        assertThat(formatFixed(22.0, 2)).isEqualTo("22.00")
        assertThat(formatFixed(7.0, 1)).isEqualTo("7.0")
    }

    @Test
    fun `healthy range endpoints render one decimal`() {
        assertThat(formatFixed(18.5, 1)).isEqualTo("18.5")
        assertThat(formatFixed(24.9, 1)).isEqualTo("24.9")
    }
}
