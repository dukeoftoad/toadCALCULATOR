// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.toadcalculator.ui.settings.SettingsScreen
import com.toadcalculator.ui.settings.SettingsTags
import com.toadcalculator.ui.theme.ToadCalculatorTheme
import com.toadcalculator.viewmodel.SettingsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val picked = mutableMapOf<String, Any>()
    private var cleared = 0

    private fun setContent(precision: Int = 12) {
        composeRule.setContent {
            ToadCalculatorTheme {
                SettingsScreen(
                    state = SettingsUiState(precision = precision),
                    onTheme = { picked["theme"] = it },
                    onPrecision = { picked["precision"] = it },
                    onAngleMode = { picked["angle"] = it },
                    onHaptics = { picked["haptics"] = it },
                    onClearHistory = { cleared++ },
                    onOpenHistorySheet = {},
                )
            }
        }
    }

    @Test
    fun shows_all_setting_groups() {
        setContent()
        composeRule.onNodeWithText("Theme").assertIsDisplayed()
        composeRule.onNodeWithText("Decimal precision (significant digits)").assertIsDisplayed()
        composeRule.onNodeWithText("Default angle mode").assertIsDisplayed()
        composeRule.onNodeWithText("Key press vibration").assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTags.CLEAR_HISTORY).assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTags.ABOUT).assertIsDisplayed()
    }

    @Test
    fun selecting_precision_reports_callback() {
        setContent()
        composeRule.onNodeWithTag(SettingsTags.precision(8)).performClick()
        assertThat(picked["precision"]).isEqualTo(8)
    }

    @Test
    fun selecting_theme_reports_callback() {
        setContent()
        composeRule.onNodeWithTag(SettingsTags.theme(com.toadcalculator.data.ThemeMode.DARK)).performClick()
        assertThat(picked["theme"]).isEqualTo(com.toadcalculator.data.ThemeMode.DARK)
    }

    @Test
    fun selecting_angle_mode_reports_callback() {
        setContent()
        composeRule.onNodeWithTag(SettingsTags.angle(com.toadcalculator.data.AngleModePref.RAD)).performClick()
        assertThat(picked["angle"]).isEqualTo(com.toadcalculator.data.AngleModePref.RAD)
    }

    @Test
    fun haptics_switch_reports_callback() {
        setContent()
        composeRule.onNodeWithTag(SettingsTags.HAPTICS).performClick()
        assertThat(picked["haptics"]).isEqualTo(false)
    }

    @Test
    fun clear_history_requires_confirmation() {
        setContent()
        composeRule.onNodeWithTag(SettingsTags.CLEAR_HISTORY).performClick()
        assertThat(cleared).isEqualTo(0)
        composeRule.onNodeWithText("Clear").performClick()
        assertThat(cleared).isEqualTo(1)
    }

    @Test
    fun about_sheet_shows_version_license_and_privacy_statement() {
        setContent()
        composeRule.onNodeWithTag(SettingsTags.ABOUT).performClick()
        composeRule.onNodeWithTag(SettingsTags.ABOUT_SHEET).assertIsDisplayed()
        composeRule.onNodeWithText("GPL-3.0-or-later", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("no permissions, no network access", substring = true).assertIsDisplayed()
    }
}
