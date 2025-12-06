package com.example.helpinghand

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.helpinghand.ui.theme.HelpingHandTheme
import com.example.helpinghand.ui.navigation.AppNavigation
import com.example.helpinghand.work.rememberIsDarkFromSensor
import com.example.helpinghand.AppLogger


class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppLogger.d(AppLogger.TAG_VM, "MainActivity.onCreate")

        val app = application as HelpingHandApp
        val settingsRepository = app.settingsRepository

        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()

            val darkMode by settingsRepository.darkModeEnabled.collectAsState(initial = false)
            val dynamicTheme by settingsRepository.dynamicThemeEnabled.collectAsState(initial = false)

            // Use our sensor helper
            val (hasLightSensor, sensorSaysDark) = rememberIsDarkFromSensor(
                dynamicEnabled = dynamicTheme
            )

            // Effective dark mode:
            val effectiveDark = if (dynamicTheme && hasLightSensor) {
                sensorSaysDark
            } else {
                darkMode
            }

            HelpingHandTheme(darkTheme = effectiveDark) {
                AppNavigation(
                    settingsRepository = settingsRepository,
                    hasLightSensor = hasLightSensor
                )
            }
        }
    }
}
