// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.toadcalculator.ui.AppRoot

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // B10: hold the system splash on screen until the first composition is
        // themed by the REAL persisted settings. Previously the splash hid at
        // the first drawable frame, which rendered the system-default palette
        // for a frame or two while DataStore's first emission was still in
        // flight — a stored DARK theme visibly flashed LIGHT on cold start.
        var uiReady = false
        installSplashScreen().setKeepOnScreenCondition { !uiReady }
        super.onCreate(savedInstanceState)
        setContent {
            AppRoot(onFirstFrame = { uiReady = true })
        }
    }
}
