// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toadcalculator.R
import com.toadcalculator.data.HistoryEntry

object HistoryTags {
    const val SHEET = "history_sheet"
    const val LIST = "history_list"
    const val EMPTY = "history_empty"
    const val CLEAR_ALL = "history_clear_all"
    fun row(id: Long) = "history_row_$id"
    fun delete(id: Long) = "history_delete_$id"
}

/**
 * History bottom sheet: newest first, tap to load back into the input,
 * swipe (or trailing button) to delete one, clear-all with confirmation.
 *
 * Phase 9.8 modern pass: entries render as tonal cards with a brand icon tile
 * (mirroring the tools home), and the clear-all confirmation dialog now lives
 * here — previously the dialog state existed but the button bypassed it (B1),
 * so one tap wiped the whole history with no recourse.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorySheet(
    entries: List<HistoryEntry>,
    visible: Boolean,
    onDismiss: () -> Unit,
    onLoad: (String) -> Unit,
    onDelete: (HistoryEntry) -> Unit,
    onClearAll: () -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag(HistoryTags.SHEET),
    ) {
        HistorySheetContent(
            entries = entries,
            onLoad = onLoad,
            onDelete = onDelete,
            onClearAll = onClearAll,
        )
    }
}

@Composable
fun HistorySheetContent(
    entries: List<HistoryEntry>,
    onLoad: (String) -> Unit,
    onDelete: (HistoryEntry) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // B1: confirmation lives with the button that triggers it. The previous
    // layout kept this state in the wrapper but never wired it, so the clear
    // button fired onClearAll directly.
    var confirmClear by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            if (entries.isNotEmpty()) {
                TextButton(
                    onClick = { confirmClear = true },
                    modifier = Modifier.testTag(HistoryTags.CLEAR_ALL),
                ) { Text(stringResource(R.string.history_clear_all)) }
            }
        }

        if (entries.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag(HistoryTags.LIST),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(entries, key = { _, e -> e.id }) { _, entry ->
                    HistoryRow(
                        entry = entry,
                        onLoad = onLoad,
                        onDelete = onDelete,
                    )
                }
                item { Spacer(Modifier.height(4.dp)) }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.confirm_clear_title)) },
            text = { Text(stringResource(R.string.confirm_clear_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearAll()
                }) { Text(stringResource(R.string.action_confirm_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "=",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.history_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 12.dp)
                .testTag(HistoryTags.EMPTY),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    onLoad: (String) -> Unit,
    onDelete: (HistoryEntry) -> Unit,
) {
    val deleteDescription = stringResource(R.string.history_delete_cd)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete(entry)
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.errorContainer),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        },
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .clickable { onLoad(entry.expression) }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag(HistoryTags.row(entry.id)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "=",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.expression,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = entry.result,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                IconButton(
                    onClick = { onDelete(entry) },
                    modifier = Modifier
                        .testTag(HistoryTags.delete(entry.id))
                        .semantics { contentDescription = deleteDescription },
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                }
            }
        }
    }
}
