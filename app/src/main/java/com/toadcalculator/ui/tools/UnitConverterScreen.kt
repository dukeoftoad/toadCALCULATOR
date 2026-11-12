// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.toadcalculator.R
import com.toadcalculator.math.tools.ConverterCategory
import com.toadcalculator.math.tools.Units
import com.toadcalculator.math.tools.convert

/**
 * Self-contained offline unit converter: category chips, from/to dropdowns,
 * swap button, live result. No shared state with the rest of the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterScreen(modifier: Modifier = Modifier) {
    var category by remember { mutableStateOf(ConverterCategory.LENGTH) }
    var fromUnit by remember { mutableStateOf("m") }
    var toUnit by remember { mutableStateOf("ft") }
    var value by remember { mutableStateOf("1") }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    val units = Units.CATALOGUE.getValue(category)
    fun switchCategory(c: ConverterCategory) {
        category = c
        val list = Units.CATALOGUE.getValue(c)
        fromUnit = list[0].id
        toUnit = list[1].id
    }

    val numeric = value.toDoubleOrNull()
    // Numeric result kept alongside the display string so the swap button can
    // carry the converted quantity into the new "from" field (B11).
    val resultValue: Double? = numeric?.let { convert(category, fromUnit, toUnit, it) }
    val result = if (resultValue != null) formatToolNumber(resultValue) else "—"
    val swapCd = stringResource(R.string.tool_swap)

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
                .testTag("tool_conv_categories"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ConverterCategory.entries.take(4).forEach { c ->
                FilterChip(
                    selected = category == c,
                    onClick = { switchCategory(c) },
                    label = { Text(categoryLabel(c)) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ConverterCategory.entries.drop(4).forEach { c ->
                FilterChip(
                    selected = category == c,
                    onClick = { switchCategory(c) },
                    label = { Text(categoryLabel(c)) },
                )
            }
        }

        ToolNumberField(
            label = stringResource(R.string.tool_value),
            value = value,
            onValueChange = { value = it },
            testTag = ToolTags.CONVERTER_VALUE,
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UnitDropdown(
                label = stringResource(R.string.tool_from),
                units = units,
                selected = fromUnit,
                expanded = fromExpanded,
                onExpandedChange = { fromExpanded = it },
                onSelect = { fromUnit = it },
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    // B11: carry the converted quantity so the swap preserves it
                    // (1 m → 3.28 ft, swap → 3.28 ft → 1 m) instead of leaving
                    // the raw "1" in the new from-unit's space.
                    val carried = resultValue
                    val f = fromUnit
                    fromUnit = toUnit
                    toUnit = f
                    if (carried != null && !carried.isNaN() && !carried.isInfinite()) {
                        val text = formatToolNumber(carried)
                        if (text.isNotEmpty() && text.matches(Regex("^\\d*\\.?\\d*$"))) value = text
                    }
                },
                modifier = Modifier
                    .testTag("tool_conv_swap")
                    .semantics { contentDescription = swapCd },
            ) {
                Text(
                    text = "⇅",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            UnitDropdown(
                label = stringResource(R.string.tool_to),
                units = units,
                selected = toUnit,
                expanded = toExpanded,
                onExpandedChange = { toExpanded = it },
                onSelect = { toUnit = it },
                modifier = Modifier.weight(1f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag(ToolTags.CONVERTER_RESULT),
        ) {
            Text(
                text = result,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "${fromSymbol(units, fromUnit)} → ${toSymbol(units, toUnit)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun fromSymbol(units: List<com.toadcalculator.math.tools.UnitDef>, id: String) =
    units.first { it.id == id }.symbol

@Composable
private fun toSymbol(units: List<com.toadcalculator.math.tools.UnitDef>, id: String) =
    units.first { it.id == id }.symbol

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdown(
    label: String,
    units: List<com.toadcalculator.math.tools.UnitDef>,
    selected: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = units.firstOrNull { it.id == selected }?.symbol ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            units.forEach { u ->
                DropdownMenuItem(
                    text = { Text(u.symbol) },
                    onClick = {
                        onSelect(u.id)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}

@Composable
internal fun categoryLabel(c: ConverterCategory): String = when (c) {
    ConverterCategory.LENGTH -> stringResource(R.string.cat_length)
    ConverterCategory.AREA -> stringResource(R.string.cat_area)
    ConverterCategory.VOLUME -> stringResource(R.string.cat_volume)
    ConverterCategory.MASS -> stringResource(R.string.cat_mass)
    ConverterCategory.TEMPERATURE -> stringResource(R.string.cat_temperature)
    ConverterCategory.SPEED -> stringResource(R.string.cat_speed)
    ConverterCategory.TIME -> stringResource(R.string.cat_time)
    ConverterCategory.DATA -> stringResource(R.string.cat_data)
}
