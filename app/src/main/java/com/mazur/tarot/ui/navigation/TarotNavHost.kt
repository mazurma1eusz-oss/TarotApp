package com.mazur.tarot.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.mazur.tarot.R
import com.mazur.tarot.data.local.datastore.AppSettings
import com.mazur.tarot.notifications.NotificationScheduler
import com.mazur.tarot.ui.components.MysticBackground
import com.mazur.tarot.ui.screens.ask.AskCardsScreen
import com.mazur.tarot.ui.screens.carddetail.CardDetailScreen
import com.mazur.tarot.ui.screens.cardofday.CardOfDayScreen
import com.mazur.tarot.ui.screens.encyclopedia.EncyclopediaScreen
import com.mazur.tarot.ui.screens.journal.JournalScreen
import com.mazur.tarot.ui.screens.onboarding.OnboardingScreen
import com.mazur.tarot.ui.screens.settings.SettingsScreen
import com.mazur.tarot.ui.screens.splash.SplashScreen
import com.mazur.tarot.ui.tarotApp
import com.mazur.tarot.ui.theme.MysticBackgroundAlt
import com.mazur.tarot.ui.theme.MysticPurple
import com.mazur.tarot.ui.theme.MysticTextPrimary
import com.mazur.tarot.ui.theme.MysticTextSecondary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TarotNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = bottomNavItems.any { it.screen.route == currentRoute }
    val showTopBar = currentRoute != Screen.Splash.route && currentRoute != Screen.Onboarding.route

    val context = LocalContext.current
    val app = tarotApp()
    val bootstrapScope = rememberCoroutineScope()
    val settings by app.settingsDataStore.settingsFlow.collectAsStateWithLifecycle(initialValue = AppSettings())

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            bootstrapScope.launch {
                val bootSettings = app.settingsDataStore.settingsFlow.first()
                NotificationScheduler.ensureChannel(context)
                NotificationScheduler.schedule(context, bootSettings.reminderHour, bootSettings.reminderMinute)
            }
        }
    }
    LaunchedEffect(Unit) {
        // Powiadomienia o Karcie Dnia są domyślnie włączone (8:00) - sprawdzamy dostęp
        // od razu przy starcie i w razie potrzeby prosimy o zgodę, zamiast czekać, aż
        // użytkownik sam włączy przełącznik w Ustawieniach.
        val bootSettings = app.settingsDataStore.settingsFlow.first()
        if (bootSettings.reminderEnabled) {
            val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                NotificationScheduler.ensureChannel(context)
                NotificationScheduler.schedule(context, bootSettings.reminderHour, bootSettings.reminderMinute)
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    MysticBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MysticTextPrimary,
            topBar = {
                // Nazwa aplikacji jest już częścią grafiki tła (main_bg), więc nie duplikujemy
                // jej osobnym tekstem - ale pasek wciąż musi rezerwować wysokość, inaczej
                // treść ekranu (nagłówki) nachodzi na dekoracyjny tytuł na grafice tła.
                if (showTopBar) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .height(64.dp)
                            .padding(end = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = {
                            bootstrapScope.launch {
                                app.settingsDataStore.setMusicMuted(!settings.musicMuted)
                            }
                        }) {
                            Icon(
                                if (settings.musicMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                contentDescription = stringResource(
                                    if (settings.musicMuted) R.string.action_unmute_music else R.string.action_mute_music,
                                ),
                                tint = MysticTextSecondary,
                            )
                        }
                        IconButton(onClick = {
                            if (currentRoute != Screen.Settings.route) {
                                navController.navigate(Screen.Settings.route)
                            }
                        }) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.nav_settings),
                                tint = MysticTextSecondary,
                            )
                        }
                    }
                }
            },
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(containerColor = MysticBackgroundAlt) {
                        val currentDestination = backStackEntry?.destination
                        bottomNavItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                                label = { Text(stringResource(item.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MysticTextPrimary,
                                    selectedTextColor = MysticTextPrimary,
                                    unselectedIconColor = MysticTextSecondary,
                                    unselectedTextColor = MysticTextSecondary,
                                    indicatorColor = MysticPurple.copy(alpha = 0.28f),
                                ),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(Screen.Splash.route) {
                    SplashScreen(onFinished = {
                        bootstrapScope.launch {
                            val bootSettings = app.settingsDataStore.settingsFlow.first()
                            val destination = if (bootSettings.hasCompletedOnboarding) {
                                Screen.CardOfDay.route
                            } else {
                                Screen.Onboarding.route
                            }
                            navController.navigate(destination) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    })
                }
                composable(Screen.Onboarding.route) {
                    OnboardingScreen(onFinished = {
                        navController.navigate(Screen.CardOfDay.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    })
                }
                composable(Screen.CardOfDay.route) {
                    CardOfDayScreen()
                }
                composable(Screen.Ask.route) {
                    AskCardsScreen(onProRequired = { navController.navigate(Screen.Settings.route) })
                }
                composable(Screen.Encyclopedia.route) {
                    EncyclopediaScreen(onCardClick = { cardId ->
                        navController.navigate(Screen.CardDetail.createRoute(cardId))
                    })
                }
                composable(Screen.Journal.route) {
                    JournalScreen(onUnlockPro = { navController.navigate(Screen.Settings.route) })
                }
                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
                composable(
                    route = Screen.CardDetail.route,
                    arguments = listOf(navArgument(Screen.CardDetail.ARG_CARD_ID) { type = NavType.IntType }),
                ) { entry ->
                    val cardId = entry.arguments?.getInt(Screen.CardDetail.ARG_CARD_ID) ?: -1
                    CardDetailScreen(cardId = cardId, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
