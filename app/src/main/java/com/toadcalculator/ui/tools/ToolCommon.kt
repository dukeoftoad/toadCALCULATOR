// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.toadcalculator.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object ToolTags {
    const val CONVERTER_VALUE = "tool_conv_value"
    const val CONVERTER_RESULT = "tool_conv_result"
    const val BMI_HEIGHT = "tool_bmi_height"
    const val BMI_WEIGHT = "tool_bmi_weight"
    const val BMI_RESULT = "tool_bmi_result"
    const val AGE_RESULT = "tool_age_result"
    const val DIFF_RESULT = "tool_diff_result"
}

/** Landing list for the four offline tools. */
@Composable
fun ToolsHomeScreen(
    onOpen: (ToolDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        ToolDestination.entries.forEach { dest ->
            ListItem(
                headlineContent = { Text(toolTitle(dest)) },
                supportingContent = { Text(toolSubtitle(dest)) },
                modifier = Modifier
                    .clickable { onOpen(dest) }
                    .testTag("tool_open_${dest.name.lowercase()}"),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

enum class ToolDestination { UNIT_CONVERTER, BMI, AGE, DATE_DIFF }

@Composable
fun toolTitle(dest: ToolDestination): String = when (dest) {
    ToolDestination.UNIT_CONVERTER -> stringResource(R.string.tool_converter_title)
    ToolDestination.BMI -> stringResource(R.string.tool_bmi_title)
    ToolDestination.AGE -> stringResource(R.string.tool_age_title)
    ToolDestination.DATE_DIFF -> stringResource(R.string.tool_diff_title)
}

@Composable
private fun toolSubtitle(dest: ToolDestination): String = when (dest) {
    ToolDestination.UNIT_CONVERTER -> stringResource(R.string.tool_converter_subtitle)
    ToolDestination.BMI -> stringResource(R.string.tool_bmi_subtitle)
    ToolDestination.AGE -> stringResource(R.string.tool_age_subtitle)
    ToolDestination.DATE_DIFF -> stringResource(R.string.tool_diff_subtitle)
}

/** Shared numeric input field. */
@Composable
internal fun ToolNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text ->
            // Allow only a sane decimal literal while typing.
            if (text.isEmpty() || text.matches(Regex("^\\d*\\.?\\d*$"))) onValueChange(text)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
    )
}

private val DATE_DISPLAY: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@Composable
internal fun formatDate(date: LocalDate): String = DATE_DISPLAY.format(date)

/** Two-column row helper used by tool results. */
@Composable
internal fun ResultLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
