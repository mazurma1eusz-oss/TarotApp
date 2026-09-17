package com.mazur.tarot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mazur.tarot.ui.navigation.TarotNavHost
import com.mazur.tarot.ui.theme.TarotAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TarotAppTheme {
                TarotNavHost()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as TarotApplication).musicManager.onForeground()
    }

    override fun onPause() {
        super.onPause()
        (application as TarotApplication).musicManager.onBackground()
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as TarotApplication).musicManager.release()
    }
}
