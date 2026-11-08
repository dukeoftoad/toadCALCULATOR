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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.toadcalculator.R
import com.toadcalculator.math.tools.Bmi
import com.toadcalculator.math.tools.BmiCategory

enum class HeightUnit { CM, M, FT_IN }
enum class WeightUnit { KG, LB }

/**
 * Self-contained BMI screen: height in cm, m, or ft+in; weight in kg or lb;
 * live BMI, WHO category and healthy weight range.
 */
@Composable
fun BmiScreen(modifier: Modifier = Modifier) {
    var heightUnit by remember { mutableStateOf(HeightUnit.CM) }
    var weightUnit by remember { mutableStateOf(WeightUnit.KG) }
    var heightValue by remember { mutableStateOf("175") }
    var heightFeet by remember { mutableStateOf("5") }
    var heightInches by remember { mutableStateOf("9") }
    var weightValue by remember { mutableStateOf("70") }

    val heightMeters = when (heightUnit) {
        HeightUnit.CM -> heightValue.toDoubleOrNull()?.div(100.0)
        HeightUnit.M -> heightValue.toDoubleOrNull()
        HeightUnit.FT_IN -> {
            val ft = heightFeet.toDoubleOrNull()
            val inch = heightInches.toDoubleOrNull()
            if (ft != null && inch != null) Bmi.feetInchesToMeters(ft, inch) else null
        }
    }
    val weightKg = when (weightUnit) {
        WeightUnit.KG -> weightValue.toDoubleOrNull()
        WeightUnit.LB -> weightValue.toDoubleOrNull()?.let { Bmi.poundsToKg(it) }
    }

    val bmiValue = if (heightMeters != null && weightKg != null) Bmi.bmi(heightMeters, weightKg) else Double.NaN
    // B4: only classify a real BMI — NaN must not render a misleading
    // "Underweight" label beneath the "—" placeholder.
    val category = if (!bmiValue.isNaN()) Bmi.category(bmiValue) else null
    // B5: guard against height ≤ 0 (would yield a 0.0–0.0 kg "healthy" range).
    val healthy = if (heightMeters != null && heightMeters > 0) Bmi.healthyWeightRangeKg(heightMeters) else null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.tool_height), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeightUnit.entries.forEach { u ->
                FilterChip(
                    selected = heightUnit == u,
                    onClick = { heightUnit = u },
                    label = { Text(heightLabel(u)) },
                )
            }
        }
        if (heightUnit == HeightUnit.FT_IN) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = heightFeet,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d{1,2}$"))) heightFeet = it },
                    label = { Text(stringResource(R.string.tool_feet)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag(ToolTags.BMI_HEIGHT + "_ft"),
                )
                OutlinedTextField(
                    value = heightInches,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d{1,2}(\\.\\d?)?$"))) heightInches = it },
                    label = { Text(stringResource(R.string.tool_inches)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).testTag(ToolTags.BMI_HEIGHT + "_in"),
                )
            }
        } else {
            OutlinedTextField(
                value = heightValue,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) heightValue = it },
                label = { Text(heightUnitLabel(heightUnit)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ToolTags.BMI_HEIGHT),
            )
        }

        Text(stringResource(R.string.tool_weight), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WeightUnit.entries.forEach { u ->
                FilterChip(
                    selected = weightUnit == u,
                    onClick = { weightUnit = u },
                    label = { Text(weightLabel(u)) },
                )
            }
        }
        OutlinedTextField(
            value = weightValue,
            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) weightValue = it },
            label = { Text(weightUnitLabel(weightUnit)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ToolTags.BMI_WEIGHT),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag(ToolTags.BMI_RESULT),
        ) {
            Text(
                text = if (bmiValue.isNaN()) "—" else String.format(java.util.Locale.ROOT, "%.2f", bmiValue),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            if (category != null) {
                Text(
                    text = categoryLabel(category),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            if (healthy != null) {
                ResultLine(
                    label = stringResource(R.string.tool_healthy_range),
                    value = String.format(
                        java.util.Locale.ROOT,
                        "%.1f – %.1f kg",
                        healthy.start,
                        healthy.endInclusive,
                    ),
                )
            }
        }
    }
}

@Composable
private fun heightLabel(u: HeightUnit): String = when (u) {
    HeightUnit.CM -> stringResource(R.string.unit_cm)
    HeightUnit.M -> stringResource(R.string.unit_m)
    HeightUnit.FT_IN -> stringResource(R.string.unit_ftin)
}

@Composable
private fun heightUnitLabel(u: HeightUnit): String = when (u) {
    HeightUnit.CM -> stringResource(R.string.tool_height_cm)
    HeightUnit.M -> stringResource(R.string.tool_height_m)
    HeightUnit.FT_IN -> ""
}

@Composable
private fun weightLabel(u: WeightUnit): String = when (u) {
    WeightUnit.KG -> stringResource(R.string.unit_kg)
    WeightUnit.LB -> stringResource(R.string.unit_lb)
}

@Composable
private fun weightUnitLabel(u: WeightUnit): String = when (u) {
    WeightUnit.KG -> stringResource(R.string.tool_weight_kg)
    WeightUnit.LB -> stringResource(R.string.tool_weight_lb)
}

@Composable
internal fun categoryLabel(c: BmiCategory): String = when (c) {
    BmiCategory.UNDERWEIGHT -> stringResource(R.string.bmi_underweight)
    BmiCategory.NORMAL -> stringResource(R.string.bmi_normal)
    BmiCategory.OVERWEIGHT -> stringResource(R.string.bmi_overweight)
    BmiCategory.OBESE -> stringResource(R.string.bmi_obese)
}
