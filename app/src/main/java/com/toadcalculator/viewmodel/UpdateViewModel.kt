// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.toadcalculator.BuildConfig
import com.toadcalculator.update.StartupUpdateCheck
import com.toadcalculator.update.UpdateIntents
import com.toadcalculator.update.data.DownloadRequest
import com.toadcalculator.update.data.GitHubRelease
import com.toadcalculator.update.data.GithubReleaseChecker
import com.toadcalculator.update.data.SystemUpdateDownloader
import com.toadcalculator.update.data.UpdateCheckException
import com.toadcalculator.update.data.UpdateChecker
import com.toadcalculator.update.data.UpdateDownloader
import com.toadcalculator.update.domain.VersionCompare
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * State machine for the OTA check (Settings → "Check for updates"), ported from
 * toadFILES' AboutViewModel update half.
 *
 * PRIVACY STANCE: this panel contacts GitHub only on user action; the silent
 * startup check lives in [StartupUpdateCheck] and only ever SEEDS this state with
 * an already-fetched result. GitHub-only endpoints, nothing cached beyond this
 * in-memory state.
 */
sealed interface UpdateState {
    data object Idle : UpdateState

    data object Checking : UpdateState

    /** The newest GitHub release is not newer than the installed version. */
    data class UpToDate(val currentVersion: String) : UpdateState

    /**
     * A newer release exists. [downloadedUri] (a DownloadManager content URI) is set
     * once the APK finished downloading, which is when the installer handoff happens;
     * [needsInstallPermission] marks the "Install unknown apps" round-trip — the APK
     * is kept so the user can tap "Download & install" again after flipping the toggle.
     *
     * Download-phase state: [downloadProgress] is 0..1 while the APK streams (null
     * while the total size is unknown — the UI then shows an indeterminate bar);
     * [downloadError] carries the retry message when the download FAILED. A failed
     * download deliberately STAYS in Available (not Error): every field the retry
     * needs is still on screen, and the same "Download & install" button — relabeled
     * "Retry download" — re-enqueues the job. An Error state here would only offer
     * the wrong retry (re-checking GitHub for a release we already have).
     */
    data class Available(
        val version: String,
        val apkUrl: String,
        val fileName: String,
        val sizeBytes: Long,
        val notes: String?,
        val htmlUrl: String,
        val downloadInProgress: Boolean = false,
        val downloadProgress: Float? = null,
        val downloadError: String? = null,
        val downloadedUri: Uri? = null,
        val needsInstallPermission: Boolean = false,
    ) : UpdateState

    data class Error(val message: String) : UpdateState
}

/**
 * ViewModel behind the Settings "Updates" card: the user-initiated GitHub check,
 * the APK download via the system DownloadManager, and the installer handoff.
 * Ported from toadFILES' AboutViewModel (update half), adapted to this app's
 * viewmodel/ + StateFlow conventions.
 */
class UpdateViewModel(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val updateChecker: UpdateChecker = GithubReleaseChecker(),
    private val updateDownloader: UpdateDownloader = SystemUpdateDownloader(),
    private val startupCheck: StartupUpdateCheck = StartupUpdateCheck.instance,
    private val installedVersion: String = BuildConfig.VERSION_NAME,
) : ViewModel() {

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var checkJob: Job? = null
    private var downloadJob: Job? = null

    init {
        // If the silent startup check (app open) already found a newer release,
        // arrive with the update panel populated — the banner's "Update" tap
        // deep-links to Settings straight into the existing "Download & install"
        // flow. The check may still be in flight when this ViewModel is built
        // (both start with the app), so ALSO watch the banner flow and seed the
        // panel the moment the silent result lands.
        seedFromStartupCheck()
        startupCheck.banner
            .onEach { release -> if (release != null) seedFrom(release) }
            .launchIn(viewModelScope)
    }

    /** Populate the panel from the startup check's result, if any (Idle only, never downloads). */
    private fun seedFromStartupCheck() {
        startupCheck.banner.value?.let { seedFrom(it) }
    }

    /** Seeds Available from an already-fetched newer release; only ever fills an Idle panel. */
    private fun seedFrom(release: GitHubRelease) {
        if (_state.value !is UpdateState.Idle) return // never overwrite a user-driven state
        if (!VersionCompare.isNewer(installedVersion, release.tagName)) return
        if (release.apkUrl == null) return
        _state.value = UpdateState.Available(
            version = release.tagName,
            apkUrl = release.apkUrl,
            fileName = release.apkName ?: DEFAULT_APK_NAME,
            sizeBytes = release.apkSizeBytes,
            notes = release.notes,
            htmlUrl = release.htmlUrl,
        )
    }

    /**
     * Checks GitHub Releases for a version newer than the installed version.
     * Strictly user-initiated and cancellable via [viewModelScope]; a second click
     * while a check is running is a no-op (the spinner already communicates that
     * work is in flight). Dismissing the panel mid-check makes the late result a
     * no-op too.
     */
    fun checkForUpdates() {
        if (_state.value is UpdateState.Checking) return
        // Flip synchronously (not inside the coroutine) so a rapid second tap — which
        // also lands on the main thread — always sees Checking and is a no-op.
        _state.value = UpdateState.Checking
        checkJob?.cancel()
        checkJob = viewModelScope.launch(ioDispatcher) {
            val result: UpdateState = try {
                val release = updateChecker.fetchLatestRelease()
                when {
                    !VersionCompare.isNewer(installedVersion, release.tagName) ->
                        UpdateState.UpToDate(installedVersion)
                    release.apkUrl == null ->
                        UpdateState.Error(
                            "Release ${release.tagName} has no APK attached yet — " +
                                "you can grab it from GitHub."
                        )
                    else -> UpdateState.Available(
                        version = release.tagName,
                        apkUrl = release.apkUrl,
                        fileName = release.apkName ?: DEFAULT_APK_NAME,
                        sizeBytes = release.apkSizeBytes,
                        notes = release.notes,
                        htmlUrl = release.htmlUrl,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UpdateState.Error(checkErrorMessage(e))
            }
            // Only land the result if the user has not dismissed the panel meanwhile.
            _state.update { cur ->
                if (cur is UpdateState.Checking) result else cur
            }
        }
    }

    /**
     * "Download & install" tap — and the retry tap after a failed download (same
     * code path: the destination file is deleted before enqueueing, so a retry
     * re-downloads from scratch). First press: enqueues the APK in the system
     * [android.app.DownloadManager], watches it from the SAME 500 ms poll that
     * feeds the in-app progress bar, and, when it completes, hands the downloaded
     * content URI to the system installer ([UpdateIntents.installApkFromUri],
     * including the "Install unknown apps" gate and outcome handling: on
     * [UpdateIntents.InstallResult.NEEDS_PERMISSION] the downloaded APK is kept so a
     * second tap installs it without re-downloading; dead-end outcomes surface as
     * [UpdateState.Error]).
     *
     * A second tap while a download is running is a no-op; a tap after a completed
     * download goes straight to the installer handoff. A FAILED download stays in
     * Available with [UpdateState.Available.downloadError] set — the message says
     * the APK download failed and that the user can retry, and the button becomes
     * "Retry download". If the user leaves Settings mid-download the handoff is
     * cancelled with the ViewModel — the system download itself continues, and
     * re-tapping re-runs the flow.
     */
    fun downloadAndInstall(context: Context) {
        val current = _state.value as? UpdateState.Available ?: return
        if (current.downloadInProgress) return
        val appContext = context.applicationContext
        if (current.downloadedUri != null) {
            handOffToInstaller(appContext)
            return
        }
        downloadJob?.cancel()
        // Flip synchronously (not inside the coroutine) so a rapid second tap — which
        // also lands on the main thread — always sees downloadInProgress and is a no-op.
        // Clears any previous failure message and progress: a fresh attempt starts clean.
        mutateAvailable {
            it.copy(
                downloadInProgress = true,
                downloadProgress = null,
                downloadError = null,
                needsInstallPermission = false,
            )
        }
        downloadJob = viewModelScope.launch(ioDispatcher) {
            try {
                val id = updateDownloader.enqueue(
                    appContext,
                    DownloadRequest(
                        url = current.apkUrl,
                        fileName = current.fileName,
                        title = "toadCALCULATOR ${current.version}",
                        description = "toadCALCULATOR update — installed after download",
                    ),
                )
                val uri = updateDownloader.awaitDownload(appContext, id) { p ->
                    val fraction = p.fraction
                    if (fraction != null) {
                        mutateAvailable { it.copy(downloadProgress = fraction) }
                    }
                }
                if (uri == null) {
                    // DownloadManager reported failure (or the watch timed out): stay in
                    // Available with the retry message — Error would offer the wrong retry.
                    failDownload(DOWNLOAD_FAILED)
                    return@launch
                }
                mutateAvailable {
                    it.copy(
                        downloadInProgress = false,
                        downloadProgress = null,
                        downloadError = null,
                        downloadedUri = uri,
                    )
                }
                handOffToInstaller(appContext)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failDownload(downloadErrorMessage(e))
            }
        }
    }

    /** Dismisses the update panel and cancels any in-flight check/download job. */
    fun clearUpdateState() {
        checkJob?.cancel()
        checkJob = null
        downloadJob?.cancel()
        downloadJob = null
        _state.value = UpdateState.Idle
    }

    private fun handOffToInstaller(context: Context) {
        val available = _state.value as? UpdateState.Available ?: return
        val uri = available.downloadedUri ?: return
        when (UpdateIntents.installApkFromUri(context, uri)) {
            UpdateIntents.InstallResult.STARTED -> Unit // the installer took over
            UpdateIntents.InstallResult.NEEDS_PERMISSION ->
                mutateAvailable { it.copy(needsInstallPermission = true) }
            UpdateIntents.InstallResult.SETTINGS_FAILED ->
                failAvailable("Could not open Android's install-permission settings.")
            UpdateIntents.InstallResult.FAILED ->
                failAvailable("Could not start the installer for the downloaded APK.")
        }
    }

    /** Updates the Available state in place; a no-op if the panel moved on meanwhile. */
    private fun mutateAvailable(transform: (UpdateState.Available) -> UpdateState.Available) {
        _state.update { cur ->
            val available = cur as? UpdateState.Available ?: return@update cur
            cur.copy(update = transform(available))
        }
    }

    /**
     * Marks the in-place download as failed but KEEPS the release on screen: the
     * panel stays Available with [UpdateState.Available.downloadError] set, so the
     * retry tap re-runs [downloadAndInstall] instead of re-checking GitHub. A no-op
     * if the panel moved on meanwhile (dismissed, or a new check replaced the release).
     */
    private fun failDownload(message: String) {
        mutateAvailable {
            it.copy(
                downloadInProgress = false,
                downloadProgress = null,
                downloadError = message,
            )
        }
    }

    /** Replaces Available with [message] as an Error, unless the panel moved on. */
    private fun failAvailable(message: String) {
        _state.update { cur ->
            if (cur is UpdateState.Available) UpdateState.Error(message) else cur
        }
    }

    private fun checkErrorMessage(e: Exception): String = when {
        // UpdateCheckException (rate limit, 5xx, 4xx) already carries a friendly message.
        e is UpdateCheckException -> e.message ?: GENERIC_CHECK_FAILED
        e is IOException -> "Couldn't reach GitHub — check your connection and try again."
        else -> "Update check failed: ${e.message?.take(120) ?: e.javaClass.simpleName}"
    }

    private fun downloadErrorMessage(e: Exception): String = when (e) {
        is IOException -> e.message ?: DOWNLOAD_FAILED
        else -> "The download failed: ${e.message?.take(120) ?: e.javaClass.simpleName}"
    }

    companion object {
        /** Compose factory mirroring SettingsViewModel.Factory; all defaults, no graph deps. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { UpdateViewModel() }
        }

        private const val DEFAULT_APK_NAME = "toadcalculator-update.apk"
        private const val DOWNLOAD_FAILED = "The APK download failed — you can retry."
        private const val GENERIC_CHECK_FAILED = "The update check failed — try again in a moment."
    }
}
