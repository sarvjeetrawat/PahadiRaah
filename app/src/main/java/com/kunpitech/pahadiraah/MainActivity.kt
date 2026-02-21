package com.kunpitech.pahadiraah

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kunpitech.pahadiraah.ui.navigation.PahadiRaahNavGraph
import com.kunpitech.pahadiraah.ui.theme.PahadiRaahTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        // Native Android splash (shows app icon while app loads)
        // After it dismisses, our animated Compose SplashScreen takes over
        installSplashScreen()

        // Edge-to-edge: content draws behind status bar & nav bar
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        setContent {
            PahadiRaahTheme(darkTheme = true) {
                PahadiRaahNavGraph()
            }
        }
    }
}