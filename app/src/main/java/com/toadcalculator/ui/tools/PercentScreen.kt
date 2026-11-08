// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.toadcalculator.R
import com.toadcalculator.math.tools.Percent
import com.toadcalculator.math.tools.PercentMode

/**
 * Self-contained percentage tool: four common modes (what % of, % of, increase,
 * decrease), two inputs, live result. Pure offline math via [Percent].
 */
@Composable
fun PercentScreen(modifier: Modifier = Modifier) {
    var mode by remember { mutableStateOf(PercentMode.OF_TOTAL) }
    var a by remember { mutableStateOf("25") }
    var b by remember { mutableStateOf("200") }

    val aNum = a.toDoubleOrNull()
    val bNum = b.toDoubleOrNull()
    val result = if (aNum != null && bNum != null) {
        formatToolNumber(Percent.compute(mode, aNum, bNum))
    } else {
        "—"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tool_percent_modes"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PercentMode.entries.forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { mode = m },
                    label = { Text(modeLabel(m)) },
                    modifier = Modifier.testTag("tool_percent_mode_${m.name.lowercase()}"),
                )
            }
        }

        Text(
            text = modeDescription(mode),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ToolNumberField(
            label = stringResource(R.string.percent_input_a),
            value = a,
            onValueChange = { a = it },
            testTag = "tool_percent_a",
        )
        ToolNumberField(
            label = stringResource(R.string.percent_input_b),
            value = b,
            onValueChange = { b = it },
            testTag = "tool_percent_b",
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag(ToolTags.PERCENT_RESULT),
        ) {
            Text(
                text = if (mode == PercentMode.OF_TOTAL) "$result %" else result,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.percent_result),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun modeLabel(m: PercentMode): String = when (m) {
    PercentMode.OF_TOTAL -> stringResource(R.string.percent_mode_of)
    PercentMode.PORTION -> stringResource(R.string.percent_mode_portion)
    PercentMode.INCREASE -> stringResource(R.string.percent_mode_increase)
    PercentMode.DECREASE -> stringResource(R.string.percent_mode_decrease)
}

@Composable
private fun modeDescription(m: PercentMode): String = when (m) {
    PercentMode.OF_TOTAL -> stringResource(R.string.percent_mode_of)
    PercentMode.PORTION -> stringResource(R.string.percent_mode_portion)
    PercentMode.INCREASE -> stringResource(R.string.percent_mode_increase)
    PercentMode.DECREASE -> stringResource(R.string.percent_mode_decrease)
}
