package com.mazur.tarot.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.ui.graphics.vector.ImageVector
import com.mazur.tarot.R

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object CardOfDay : Screen("card_of_day")
    data object Ask : Screen("ask_cards")
    data object Encyclopedia : Screen("encyclopedia")
    data object Journal : Screen("journal")
    data object Settings : Screen("settings")

    data object CardDetail : Screen("card_detail/{cardId}") {
        fun createRoute(cardId: Int) = "card_detail/$cardId"
        const val ARG_CARD_ID = "cardId"
    }
}

data class BottomNavItem(
    val screen: Screen,
    val labelRes: Int,
    val icon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.CardOfDay, R.string.nav_card_of_day, Icons.Filled.AutoAwesome),
    BottomNavItem(Screen.Ask, R.string.nav_ask_cards, Icons.Filled.QuestionAnswer),
    BottomNavItem(Screen.Encyclopedia, R.string.nav_encyclopedia, Icons.AutoMirrored.Filled.MenuBook),
    BottomNavItem(Screen.Journal, R.string.nav_journal, Icons.Filled.Book),
)
