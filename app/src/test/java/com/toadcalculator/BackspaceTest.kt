// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator

import com.toadcalculator.ui.calculator.backspaceToken
import com.toadcalculator.ui.calculator.currentNumberHasDot
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackspaceTest {

    private fun del(text: String): String = backspaceToken(text, text.length).first

    @Test
    fun `deletes whole number run`() {
        assertThat(del("123")).isEqualTo("")
        assertThat(del("12+345")).isEqualTo("12+")
    }

    @Test
    fun `deletes decimal number including dot`() {
        assertThat(del("3.14")).isEqualTo("")
        assertThat(del("3.14+1")).isEqualTo("3.14+")
    }

    @Test
    fun `deletes scientific notation as one token`() {
        assertThat(del("1.5E-3")).isEqualTo("")
        assertThat(del("2+1E5")).isEqualTo("2+")
        assertThat(del("1E+5")).isEqualTo("")
        // A subtraction sign in front of a plain number is never consumed.
        assertThat(del("12-345")).isEqualTo("12-")
    }

    @Test
    fun `deletes function call header as one token`() {
        assertThat(del("sin(")).isEqualTo("")
        assertThat(del("2+cos(")).isEqualTo("2+")
        assertThat(del("sin(5)")).isEqualTo("sin(5")
    }

    @Test
    fun `deletes constants as one token`() {
        assertThat(del("2π")).isEqualTo("2")
        assertThat(del("2e")).isEqualTo("2")
    }

    @Test
    fun `deletes single operator character`() {
        assertThat(del("12+")).isEqualTo("12")
        assertThat(del("12*")).isEqualTo("12")
    }

    @Test
    fun `deletes closing paren as single character`() {
        assertThat(del("(1+2)")).isEqualTo("(1+2")
    }

    @Test
    fun `empty text is unchanged`() {
        assertThat(del("")).isEqualTo("")
    }

    @Test
    fun `cursor respected mid-text`() {
        val (t, c) = backspaceToken("12+345", 3)
        assertThat(t).isEqualTo("12345")
        assertThat(c).isEqualTo(2)
    }

    @Test
    fun `cursor at zero is unchanged`() {
        val (t, c) = backspaceToken("12", 0)
        assertThat(t).isEqualTo("12")
        assertThat(c).isEqualTo(0)
    }

    @Test
    fun `dot detection in current number`() {
        assertThat(currentNumberHasDot("3.14", 4)).isTrue()
        assertThat(currentNumberHasDot("3.14+2", 6)).isFalse()
        assertThat(currentNumberHasDot("", 0)).isFalse()
        assertThat(currentNumberHasDot("2+3.", 4)).isTrue()
    }
}
