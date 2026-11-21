// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.toadcalculator.R
import com.toadcalculator.viewmodel.UpdateState
import java.util.Locale

/** Test tags for the Settings update panel (Robolectric Compose tests). */
object UpdateTags {
    const val CHECK = "settings_update_check"
    const val DOWNLOAD_INSTALL = "settings_update_download"
    const val DISMISS = "settings_update_dismiss"
    const val PROGRESS_BAR = "update_progress_bar"
    const val PROGRESS_TEXT = "update_progress_text"
    const val DOWNLOAD_ERROR = "update_download_error"
}

/**
 * The update panel proper — every [UpdateState] rendered in this app's settings
 * style. Ported from toadFILES' AboutScreen UpdatePanel, adapted to the
 * strings.xml + tonal-card conventions here. All actions are explicit taps:
 * check, download & install, open GitHub, dismiss.
 */
@Composable
fun UpdatePanel(
    update: UpdateState,
    onCheck: () -> Unit,
    onDownloadInstall: () -> Unit,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    when (update) {
        is UpdateState.Idle -> {
            Text(
                text = stringResource(R.string.update_idle_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            TextButton(
                onClick = onCheck,
                modifier = Modifier.testTag(UpdateTags.CHECK),
            ) { Text(stringResource(R.string.update_check)) }
        }

        is UpdateState.Checking -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.update_checking),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        is UpdateState.UpToDate -> {
            Text(
                text = stringResource(R.string.update_up_to_date, update.currentVersion),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(10.dp))
            TextButton(
                onClick = onCheck,
                modifier = Modifier.testTag(UpdateTags.CHECK),
            ) { Text(stringResource(R.string.update_check_again)) }
        }

        is UpdateState.Available -> UpdateAvailable(
            available = update,
            onDownloadInstall = onDownloadInstall,
            onDismiss = onDismiss,
            onOpenUrl = onOpenUrl,
        )

        is UpdateState.Error -> {
            Text(
                text = update.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(10.dp))
            TextButton(
                onClick = onCheck,
                modifier = Modifier.testTag(UpdateTags.CHECK),
            ) { Text(stringResource(R.string.update_retry)) }
        }
    }
}

@Composable
private fun UpdateAvailable(
    available: UpdateState.Available,
    onDownloadInstall: () -> Unit,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    var notesExpanded by rememberSaveable { mutableStateOf(false) }
    val progressCd = stringResource(R.string.cd_update_download_progress)
    Text(
        text = stringResource(R.string.update_available_title, available.version),
        style = MaterialTheme.typography.titleSmall,
    )
    Spacer(Modifier.height(8.dp))
    UpdateRow(stringResource(R.string.update_new_version), available.version)
    UpdateRow(stringResource(R.string.update_size), formatApkSize(available.sizeBytes))
    available.notes?.let { notes ->
        Spacer(Modifier.height(8.dp))
        val lines = notes.split('\n')
        val truncated = !notesExpanded && lines.size > NOTES_PREVIEW_LINES
        Text(
            text = if (truncated) {
                lines.take(NOTES_PREVIEW_LINES).joinToString("\n") + "\n…"
            } else {
                notes
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (lines.size > NOTES_PREVIEW_LINES) {
            TextButton(onClick = { notesExpanded = !notesExpanded }) {
                Text(
                    stringResource(
                        if (notesExpanded) R.string.update_show_less else R.string.update_show_full_notes
                    )
                )
            }
        }
    }
    if (available.needsInstallPermission) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.update_needs_permission),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (available.downloadInProgress) {
        Spacer(Modifier.height(10.dp))
        // Real in-app progress. Determinate when the release's total size is known
        // (GitHub always sends Content-Length), indeterminate otherwise — the bar
        // never lies. The byte counters come from the system DownloadManager's own
        // watch loop; the notification shade keeps mirroring the same job.
        val fraction = available.downloadProgress
        if (fraction != null) {
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .testTag(UpdateTags.PROGRESS_BAR)
                    .semantics { contentDescription = progressCd },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.update_downloading_percent, (fraction * 100).toInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(UpdateTags.PROGRESS_TEXT),
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .testTag(UpdateTags.PROGRESS_BAR)
                    .semantics { contentDescription = progressCd },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.update_downloading_indeterminate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(UpdateTags.PROGRESS_TEXT),
            )
        }
    }
    if (available.downloadError != null) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = available.downloadError, // smart-cast: immutable same-module val, null-checked above
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag(UpdateTags.DOWNLOAD_ERROR),
        )
    }
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        val buttonLabel = when {
            available.downloadedUri != null -> stringResource(R.string.update_install_downloaded)
            available.downloadError != null -> stringResource(R.string.update_retry_download)
            else -> stringResource(R.string.update_download_install)
        }
        Button(
            onClick = onDownloadInstall,
            enabled = !available.downloadInProgress,
            modifier = Modifier.testTag(UpdateTags.DOWNLOAD_INSTALL),
        ) {
            Text(buttonLabel)
        }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = { onOpenUrl(available.htmlUrl) }) {
            Text(stringResource(R.string.update_view_on_github))
        }
    }
    Spacer(Modifier.height(4.dp))
    TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag(UpdateTags.DISMISS),
    ) { Text(stringResource(R.string.update_dismiss)) }
}

/** Label/value line in the update card, mirroring the settings rows. */
@Composable
internal fun UpdateRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

private const val NOTES_PREVIEW_LINES = 4

/** APK size as a plain "x.y MB" (or GB) string — the release payload's raw bytes. */
private fun formatApkSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) {
        String.format(Locale.US, "%.1f GB", mb / 1024)
    } else {
        String.format(Locale.US, "%.1f MB", mb)
    }
}
