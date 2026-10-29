// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.toadcalculator.ui.AppRoot
import com.toadcalculator.ui.theme.ToadCalculatorTheme
import com.toadcalculator.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {

    private val appGraph: AppGraph by lazy { AppGraph(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            ToadCalculatorTheme(themeMode = ThemeMode.SYSTEM) {
                AppRoot(appGraph = appGraph)
            }
        }
    }
}
