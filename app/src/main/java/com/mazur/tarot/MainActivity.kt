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
        val app = application as TarotApplication
        app.musicManager.onForeground()
        // Wygasła/anulowana subskrypcja nie cofa sama z siebie stanu Premium w apce - Google
        // Play po prostu przestaje zwracać ten zakup w queryPurchasesAsync, ale to wywołanie
        // dzieje się tylko raz przy starcie połączenia (patrz BillingManager.startConnection).
        // Odświeżamy więc przy KAŻDYM powrocie na pierwszy plan, żeby zmiana widoczna w Google
        // Play (np. "subskrypcja wygasła") trafiła też do apki bez konieczności jej zabijania.
        app.billingManager.refreshPurchasesIfConnected()
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
