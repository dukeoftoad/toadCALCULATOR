// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.toadcalculator.data.HistoryEntry
import com.toadcalculator.ui.history.HistorySheetContent
import com.toadcalculator.ui.history.HistoryTags
import com.toadcalculator.ui.theme.ToadCalculatorTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistoryUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val loaded = mutableListOf<String>()
    private val deleted = mutableListOf<Long>()
    private var cleared = 0

    private val entries = listOf(
        HistoryEntry(id = 2, expression = "2*21", result = "42", timestamp = 2000),
        HistoryEntry(id = 1, expression = "1+1", result = "2", timestamp = 1000),
    )

    private fun setContent(vararg list: HistoryEntry) {
        composeRule.setContent {
            ToadCalculatorTheme {
                HistorySheetContent(
                    entries = list.toList(),
                    onLoad = { loaded += it },
                    onDelete = { deleted += it.id },
                    onClearAll = { cleared++ },
                )
            }
        }
    }

    @Test
    fun shows_entries_newest_first() {
        setContent(entries[0], entries[1])
        composeRule.onNodeWithText("2*21").assertIsDisplayed()
        composeRule.onNodeWithText("42").assertIsDisplayed()
        composeRule.onNodeWithText("1+1").assertIsDisplayed()
    }

    @Test
    fun shows_empty_state_when_no_entries() {
        setContent()
        composeRule.onNodeWithTag(HistoryTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText("Clear all").assertDoesNotExist()
    }

    @Test
    fun tapping_an_entry_loads_it_back() {
        setContent(entries[0], entries[1])
        composeRule.onNodeWithTag(HistoryTags.row(1)).performClick()
        assertThat(loaded).containsExactly("1+1")
    }

    @Test
    fun delete_button_removes_single_entry() {
        setContent(entries[0], entries[1])
        composeRule.onNodeWithTag(HistoryTags.delete(2)).performClick()
        assertThat(deleted).containsExactly(2L)
        assertThat(cleared).isEqualTo(0)
    }

    @Test
    fun clear_all_button_signals_clear() {
        setContent(entries[0], entries[1])
        composeRule.onNodeWithTag(HistoryTags.CLEAR_ALL).performClick()
        assertThat(cleared).isEqualTo(1)
    }
}
