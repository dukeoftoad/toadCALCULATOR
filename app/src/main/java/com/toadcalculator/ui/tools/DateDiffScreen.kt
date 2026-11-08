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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.toadcalculator.R
import com.toadcalculator.math.tools.DateDiff
import java.time.LocalDate

/**
 * Self-contained date difference: two dates, inclusive/exclusive toggle,
 * output in days, weeks + remainder, and years/months/days.
 */
@Composable
fun DateDiffScreen(modifier: Modifier = Modifier) {
    var start by remember { mutableStateOf(LocalDate.of(2025, 1, 1)) }
    var end by remember { mutableStateOf(LocalDate.of(2025, 12, 31)) }
    var inclusive by remember { mutableStateOf(false) }

    val result = if (!end.plusDays(if (inclusive) 1 else 0).isBefore(start)) {
        DateDiff.difference(start, end, inclusive)
    } else {
        null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ToolDateField(
            label = stringResource(R.string.tool_start_date),
            date = start,
            onPick = { start = it },
            testTag = "tool_diff_start",
        )
        ToolDateField(
            label = stringResource(R.string.tool_end_date),
            date = end,
            onPick = { end = it },
            testTag = "tool_diff_end",
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = !inclusive,
                onClick = { inclusive = false },
                label = { Text(stringResource(R.string.tool_exclusive)) },
                modifier = Modifier.testTag("tool_diff_exclusive"),
            )
            FilterChip(
                selected = inclusive,
                onClick = { inclusive = true },
                label = { Text(stringResource(R.string.tool_inclusive)) },
                modifier = Modifier.testTag("tool_diff_inclusive"),
            )
        }

        if (result != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag(ToolTags.DIFF_RESULT),
            ) {
                Text(
                    text = stringResource(R.string.tool_diff_days_value, result.totalDays),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                ResultLine(
                    label = stringResource(R.string.tool_diff_weeks),
                    value = stringResource(
                        R.string.tool_diff_weeks_value,
                        result.weeks,
                        result.remainderDays,
                    ),
                )
                ResultLine(
                    label = stringResource(R.string.tool_diff_ymd),
                    value = stringResource(
                        R.string.tool_diff_ymd_value,
                        result.years,
                        result.months,
                        result.days,
                    ),
                )
            }
        } else {
            // F10: previously the result simply vanished when end < start —
            // show why instead of leaving the user guessing.
            Text(
                text = stringResource(R.string.tool_diff_inverted),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}
