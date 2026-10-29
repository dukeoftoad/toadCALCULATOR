// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.toadcalculator.R
import com.toadcalculator.math.tools.AgeCalc
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Read-only date field that opens a Material3 DatePickerDialog on tap.
 * Milliseconds travel in UTC so the picked LocalDate is stable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ToolDateField(
    label: String,
    date: LocalDate,
    onPick: (LocalDate) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth().testTag(testTag)) {
        OutlinedTextField(
            value = formatDate(date),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
        )
        Surface(
            onClick = { open = true },
            color = Color.Transparent,
            modifier = Modifier
                .matchParentSize()
                .testTag("${testTag}_tap"),
        ) { }
    }

    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    open = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

/** Self-contained age calculator: birth date → years/months/days, totals, next birthday. */
@Composable
fun AgeScreen(modifier: Modifier = Modifier) {
    var birth by remember { mutableStateOf(LocalDate.of(2000, 1, 1)) }
    var asOf by remember { mutableStateOf(LocalDate.now()) }

    val result = if (!asOf.isBefore(birth)) AgeCalc.age(birth, asOf) else null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ToolDateField(
            label = stringResource(R.string.tool_birth_date),
            date = birth,
            onPick = { birth = it },
            testTag = "tool_age_birth",
        )
        ToolDateField(
            label = stringResource(R.string.tool_asof_date),
            date = asOf,
            onPick = { asOf = it },
            testTag = "tool_age_asof",
        )

        if (result != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag(ToolTags.AGE_RESULT),
            ) {
                Text(
                    text = stringResource(
                        R.string.tool_age_summary,
                        result.years,
                        result.months,
                        result.days,
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                ResultLine(
                    label = stringResource(R.string.tool_total_days),
                    value = result.totalDays.toString(),
                )
                ResultLine(
                    label = stringResource(R.string.tool_next_birthday),
                    value = stringResource(
                        R.string.tool_next_birthday_value,
                        result.daysUntilNextBirthday,
                        formatDate(result.nextBirthdayDate),
                    ),
                )
            }
        }
    }
}
