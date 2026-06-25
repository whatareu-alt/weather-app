package com.example.aiweathermonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.aiweathermonitor.theme.AIWeatherMonitorTheme
import com.example.aiweathermonitor.ui.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        setContent {
            val viewModel: com.example.aiweathermonitor.ui.main.MainScreenViewModel =
                org.koin.androidx.compose.koinViewModel()
            val settings by viewModel.appSettings.collectAsState()

            val darkTheme = when (settings.theme) {
                AppTheme.DARK -> true
                AppTheme.LIGHT -> false
                else -> isSystemInDarkTheme()   // AppTheme.SYSTEM
            }
            AIWeatherMonitorTheme(darkTheme = darkTheme) {
                Surface(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                    MainNavigation()
                }
            }
        }
    }
}
