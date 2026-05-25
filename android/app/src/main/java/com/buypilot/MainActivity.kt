package com.buypilot

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
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
            MaterialTheme {
                BuyPilotStartupHost(showStartupSplash = !skipStartupSplash)
            }
        }
    }
}
