// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataStoreSettingsRepositoryTest {

    private lateinit var repo: DataStoreSettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repo = DataStoreSettingsRepository(context)
    }

    @Test
    fun defaults_are_applied_when_unset() = runTest {
        assertThat(repo.theme.first()).isEqualTo(ThemeMode.SYSTEM)
        assertThat(repo.precision.first()).isEqualTo(12)
        assertThat(repo.angleMode.first()).isEqualTo(AngleModePref.DEG)
        assertThat(repo.haptics.first()).isTrue()
    }

    @Test
    fun settings_survive_restart_within_same_process_store() = runTest {
        repo.setTheme(ThemeMode.DARK)
        repo.setPrecision(8)
        repo.setAngleMode(AngleModePref.RAD)
        repo.setHaptics(false)

        // A fresh instance reads the same DataStore file.
        val fresh = DataStoreSettingsRepository(ApplicationProvider.getApplicationContext())
        assertThat(fresh.theme.first()).isEqualTo(ThemeMode.DARK)
        assertThat(fresh.precision.first()).isEqualTo(8)
        assertThat(fresh.angleMode.first()).isEqualTo(AngleModePref.RAD)
        assertThat(fresh.haptics.first()).isFalse()
    }

    @Test
    fun unsupported_precision_is_rejected() = runTest {
        try {
            repo.setPrecision(7)
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // ok
        }
        assertThat(repo.precision.first()).isEqualTo(12)
    }
}
