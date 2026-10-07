package com.zed.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zed.app.R
import com.zed.app.feature.credits.CreditEditorScreen
import com.zed.app.feature.credits.CreditsScreen
import com.zed.app.feature.finance.FinanceCalendarScreen
import com.zed.app.feature.finance.FinanceScreen
import com.zed.app.feature.finance.TransactionEditorScreen
import com.zed.app.feature.habits.HabitEditorScreen
import com.zed.app.feature.habits.HabitStatsScreen
import com.zed.app.feature.habits.HabitsScreen
import com.zed.app.feature.onboarding.OnboardingScreen
import com.zed.app.feature.player.AllTracksScreen
import com.zed.app.feature.player.LikedTracksScreen
import com.zed.app.feature.player.PlayerScreen
import com.zed.app.feature.player.PlaylistsScreen
import com.zed.app.feature.search.SearchScreen
import com.zed.app.feature.settings.SettingsScreen
import com.zed.app.ui.components.ZedTopBar
import com.zed.app.ui.navigation.LocalZedActions
import com.zed.app.ui.navigation.ZedActions
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedSpacing

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    data object Habits : Screen("habits", R.string.tab_habits, Icons.Outlined.CheckCircle)
    data object Finance : Screen("finance", R.string.tab_finance, Icons.Outlined.AccountBalanceWallet)
    data object Credits : Screen("credits", R.string.tab_credits, Icons.Outlined.CalendarMonth)

    companion object {
        fun tabs() = listOf(Habits, Finance, Credits)
    }
}

@Composable
fun ZedNavHost(startDestination: String) {
    val navController = rememberNavController()
    val tabs = Screen.tabs()
    val tabRoutes = tabs.map { it.route }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in tabRoutes
    val colors = LocalZedColors.current

    CompositionLocalProvider(
        LocalZedActions provides ZedActions(
            openSearch = { navController.navigate("search") }
        )
    ) {
        Scaffold(
            containerColor = colors.background,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(containerColor = colors.background, contentColor = colors.textPrimary) {
                        tabs.forEach { screen ->
                            val selected = currentRoute == screen.route
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = null) },
                                label = {
                                    Text(
                                        text = stringResource(screen.titleRes),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                selected = selected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.accent,
                                    selectedTextColor = colors.accent,
                                    unselectedIconColor = colors.textSecondary,
                                    unselectedTextColor = colors.textSecondary,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                        NavigationBarItem(
                            icon = { Icon(Icons.Outlined.MusicNote, contentDescription = null) },
                            label = {
                                Text(
                                    text = stringResource(R.string.tab_player),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            selected = false,
                            onClick = { navController.navigate("player_screen") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.accent,
                                selectedTextColor = colors.accent,
                                unselectedIconColor = colors.textSecondary,
                                unselectedTextColor = colors.textSecondary,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 12 } },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(180)) + slideOutVertically(tween(180)) { it / 12 } }
            ) {
                composable("onboarding") {
                    OnboardingScreen(
                        onFinished = {
                            navController.navigate(Screen.Habits.route) {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    )
                }

                // Привычки + редактор + статистика
                composable(Screen.Habits.route) {
                    HabitsScreen(
                        onOpenSettings = { navController.navigate("settings") },
                        onOpenEditor = { id -> navController.navigate("habit_editor/$id") },
                        onOpenStats = { navController.navigate("habits_stats") }
                    )
                }
                composable(
                    route = "habit_editor/{habitId}",
                    arguments = listOf(navArgument("habitId") {
                        type = NavType.IntType
                        defaultValue = -1
                    })
                ) { entry ->
                    HabitEditorScreen(
                        habitId = entry.arguments?.getInt("habitId") ?: -1,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("habits_stats") {
                    HabitStatsScreen(onBack = { navController.popBackStack() })
                }

                // Финансы: список + календарь + редактор с датой/типом
                composable(Screen.Finance.route) {
                    FinanceScreen(
                        onOpenSettings = { navController.navigate("settings") },
                        onOpenEditor = { navController.navigate("finance_editor") },
                        onOpenCalendar = { dateMillis -> navController.navigate("finance_calendar?date=$dateMillis") }
                    )
                }
                composable(
                    route = "finance_calendar?date={date}",
                    arguments = listOf(navArgument("date") {
                        type = NavType.LongType
                        defaultValue = -1L
                    })
                ) {
                    FinanceCalendarScreen(
                        onBack = { navController.popBackStack() },
                        onAddTransaction = { dateMillis, type ->
                            navController.navigate("finance_editor?date=$dateMillis&type=$type")
                        }
                    )
                }
                composable(
                    route = "finance_editor?date={date}&type={type}",
                    arguments = listOf(
                        navArgument("date") {
                            type = NavType.LongType
                            defaultValue = -1L
                        },
                        navArgument("type") {
                            type = NavType.IntType
                            defaultValue = 0
                        }
                    )
                ) {
                    TransactionEditorScreen(onBack = { navController.popBackStack() })
                }

                // Кредиты
                composable(Screen.Credits.route) {
                    CreditsScreen(
                        onOpenSettings = { navController.navigate("settings") },
                        onOpenEditor = { id -> navController.navigate("credit_editor/$id") }
                    )
                }
                composable(
                    route = "credit_editor/{creditId}",
                    arguments = listOf(navArgument("creditId") {
                        type = NavType.IntType
                        defaultValue = -1
                    })
                ) { entry ->
                    CreditEditorScreen(
                        creditId = entry.arguments?.getInt("creditId") ?: -1,
                        onBack = { navController.popBackStack() }
                    )
                }

                // Плеер
                composable("player_screen") {
                    PlayerScreen(
                        onBack = { navController.popBackStack() },
                        onOpenSettings = { navController.navigate("settings") },
                        onOpenSearch = { navController.navigate("search") },
                        onOpenAllTracks = { navController.navigate("all_tracks_screen") },
                        onOpenPlaylists = { navController.navigate("playlists_screen") },
                        onOpenLiked = { navController.navigate("liked_tracks_screen") }
                    )
                }
                composable("all_tracks_screen") {
                    AllTracksScreen(
                        onBack = { navController.popBackStack() },
                        onPlayed = { navController.popBackStack() }
                    )
                }
                composable("liked_tracks_screen") {
                    LikedTracksScreen(
                        onBack = { navController.popBackStack() },
                        onPlayed = { navController.popBackStack() }
                    )
                }
                composable("playlists_screen") {
                    PlaylistsScreen(
                        onBack = { navController.popBackStack() },
                        onPlayed = { navController.popBackStack() }
                    )
                }

                // Поиск
                composable("search") {
                    SearchScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateToTab = { route ->
                            if (route == "player_screen") {
                                if (!navController.popBackStack("player_screen", false)) {
                                    navController.navigate("player_screen")
                                }
                            } else {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }

                // Настройки
                composable("settings") {
                    SettingsScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
