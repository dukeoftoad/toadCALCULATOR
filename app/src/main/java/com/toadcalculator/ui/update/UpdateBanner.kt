// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.toadcalculator.R
import com.toadcalculator.update.data.GitHubRelease

/** Test tags for the update banner (Robolectric Compose tests). */
object UpdateBannerTags {
    const val BANNER = "update_banner"
    const val ACTION = "update_banner_action"
    const val DISMISS = "update_banner_dismiss"
}

/**
 * The subtle, dismissible "update available" card shown once on app open when the
 * silent startup check found a newer release — ported from toadFILES
 * (update/UpdateBanner.kt), restyled to this app's tonal-card language.
 *
 * Non-intrusive by design: a single quiet line above the calculator, no dialog, no
 * sound, nothing downloads until the user acts. "Update" deep-links to the Settings
 * update panel, whose panel arrives already seeded with this release
 * (UpdateViewModel), so the familiar "Download & install" sheet is one tap away.
 */
@Composable
fun UpdateBanner(
    release: GitHubRelease?,
    onOpenUpdateFlow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (release == null) return // nothing to say — stays fully invisible
    val dismissCd = stringResource(R.string.cd_update_banner_dismiss)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(UpdateBannerTags.BANNER),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.update_banner_title, release.tagName),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.update_banner_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            TextButton(
                onClick = onOpenUpdateFlow,
                modifier = Modifier.testTag(UpdateBannerTags.ACTION),
            ) {
                Text(stringResource(R.string.update_banner_action))
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .testTag(UpdateBannerTags.DISMISS)
                    .semantics { contentDescription = dismissCd },
            ) {
                Icon(Icons.Outlined.Close, contentDescription = null)
            }
        }
    }
}
