// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui.tools

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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

fun toolCardTag(dest: ToolDestination): String = "tool_open_${dest.name.lowercase()}"

/** Landing list for the four offline tools: tonal cards with brand icon tiles. */
@Composable
fun ToolsHomeScreen(
    onOpen: (ToolDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    // One shared driver animation; each card derives a staggered progress window
    // from it, so the whole list fades/slides in over ~0.5s. Finite duration —
    // safe for Robolectric idle waiting.
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(durationMillis = 380, easing = FastOutSlowInEasing))
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.tools_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 16.dp),
        )
        ToolDestination.entries.forEachIndexed { index, dest ->
            ToolCard(
                dest = dest,
                appearFraction = appear.value,
                index = index,
                onOpen = onOpen,
            )
            if (index < ToolDestination.entries.lastIndex) Spacer(Modifier.height(12.dp))
        }
        Text(
            text = stringResource(R.string.tools_footnote),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp, bottom = 24.dp),
        )
    }
}

@Composable
private fun ToolCard(
    dest: ToolDestination,
    appearFraction: Float,
    index: Int,
    onOpen: (ToolDestination) -> Unit,
) {
    Surface(
        onClick = { onOpen(dest) },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                // Staggered entrance: card i starts at fraction i*0.08 of the drive.
                val start = index * 0.08f
                val progress = ((appearFraction - start) / (1f - start)).coerceIn(0f, 1f)
                alpha = progress
                translationY = (1f - progress) * 20.dp.toPx()
            }
            .testTag(toolCardTag(dest)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(toolIconRes(dest)),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = toolTitle(dest),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = toolSubtitle(dest),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(22.dp),
            )
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

/** Brand icon for each tool card (custom stroke vectors, no extended icons dep). */
fun toolIconRes(dest: ToolDestination): Int = when (dest) {
    ToolDestination.UNIT_CONVERTER -> R.drawable.ic_tool_convert
    ToolDestination.BMI -> R.drawable.ic_tool_bmi
    ToolDestination.AGE -> R.drawable.ic_tool_age
    ToolDestination.DATE_DIFF -> R.drawable.ic_tool_datadiff
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
