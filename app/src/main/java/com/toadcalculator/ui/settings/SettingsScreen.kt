// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.toadcalculator.BuildConfig
import com.toadcalculator.R
import com.toadcalculator.data.AngleModePref
import com.toadcalculator.data.ThemeMode
import com.toadcalculator.viewmodel.SettingsUiState

object SettingsTags {
    fun theme(mode: ThemeMode) = "settings_theme_${mode.name.lowercase()}"
    fun precision(p: Int) = "settings_precision_$p"
    fun angle(a: AngleModePref) = "settings_angle_${a.name.lowercase()}"
    const val HAPTICS = "settings_haptics"
    const val CLEAR_HISTORY = "settings_clear_history"
    const val ABOUT = "settings_about"
    const val ABOUT_SHEET = "about_sheet"
}

/** Settings screen: theme, precision, angle mode, haptics, clear history, about. */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onTheme: (ThemeMode) -> Unit,
    onPrecision: (Int) -> Unit,
    onAngleMode: (AngleModePref) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onOpenHistorySheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmClear by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PreferenceLabel(stringResource(R.string.settings_theme))
        ChipRow {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.theme == mode,
                    onClick = { onTheme(mode) },
                    label = { Text(themeLabel(mode)) },
                    modifier = Modifier.testTag(SettingsTags.theme(mode)),
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferenceLabel(stringResource(R.string.settings_precision))
        ChipRow {
            listOf(6, 8, 10, 12, 15).forEach { p ->
                FilterChip(
                    selected = state.precision == p,
                    onClick = { onPrecision(p) },
                    label = { Text(p.toString()) },
                    modifier = Modifier.testTag(SettingsTags.precision(p)),
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferenceLabel(stringResource(R.string.settings_angle_mode))
        ChipRow {
            AngleModePref.entries.forEach { a ->
                FilterChip(
                    selected = state.angleMode == a,
                    onClick = { onAngleMode(a) },
                    label = { Text(a.name) },
                    modifier = Modifier.testTag(SettingsTags.angle(a)),
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_haptics),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = state.haptics,
                onCheckedChange = onHaptics,
                modifier = Modifier.testTag(SettingsTags.HAPTICS),
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        TextButton(
            onClick = { confirmClear = true },
            modifier = Modifier.testTag(SettingsTags.CLEAR_HISTORY),
        ) { Text(stringResource(R.string.settings_clear_history)) }

        TextButton(
            onClick = { showAbout = true },
            modifier = Modifier.testTag(SettingsTags.ABOUT),
        ) { Text(stringResource(R.string.settings_about)) }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.confirm_clear_title)) },
            text = { Text(stringResource(R.string.confirm_clear_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearHistory()
                }) { Text(stringResource(R.string.action_confirm_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showAbout) {
        AboutSheet(onDismiss = { showAbout = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag(SettingsTags.ABOUT_SHEET),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The toad — brand signature above the wordmark.
            Icon(
                painter = painterResource(R.drawable.ic_toad_logo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 4.dp)
                    .size(56.dp),
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)) {
                        append("toad")
                    }
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(stringResource(R.string.app_name).removePrefix("toad"))
                    }
                },
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.about_privacy),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.about_license),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.about_copyright),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
}

@Composable
private fun PreferenceLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ChipRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}
