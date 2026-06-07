package com.buypilot

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.ReportDrawnWhen
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.buypilot.ui.BuyPilotStartupHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_SKIP_STARTUP_SPLASH = "com.buypilot.extra.SKIP_STARTUP_SPLASH"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        val skipStartupSplash = intent.getBooleanExtra(EXTRA_SKIP_STARTUP_SPLASH, false)
        setContent {
            var homeReadyForDraw by rememberSaveable { mutableStateOf(skipStartupSplash) }
            ReportDrawnWhen { homeReadyForDraw }
            MaterialTheme {
                BuyPilotStartupHost(
                    showStartupSplash = !skipStartupSplash,
                    onHomeReadyForDraw = { homeReadyForDraw = true },
                )
            }
        }
    }
}
