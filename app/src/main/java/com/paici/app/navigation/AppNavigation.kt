package com.paici.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.paici.app.AppViewModel
import com.paici.app.ui.camera.CameraScreen
import com.paici.app.ui.home.HomeScreen
import com.paici.app.ui.settings.SettingsScreen
import com.paici.app.ui.wordbook.DayDetailScreen
import com.paici.app.ui.wordbook.WordbookScreen
import java.time.Instant
import java.time.ZoneId

sealed class Screen(val route: String) {
    object Home        : Screen("home")
    object Camera      : Screen("camera")
    object WordBook    : Screen("wordbook")
    object WordBookDay : Screen("wordbook/{date}")
    object Settings    : Screen("settings")
}

private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home,     "首页",  Icons.Default.Home),
    BottomNavItem(Screen.WordBook, "词汇本", Icons.Default.MenuBook),
    BottomNavItem(Screen.Settings, "设置",  Icons.Default.Settings),
)

@Composable
fun AppNavigation(viewModel: AppViewModel) {
    val navController  = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute   = backStackEntry?.destination?.route

    val bottomBarRoutes = setOf(Screen.Home.route, Screen.WordBook.route, Screen.Settings.route)

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.screen.route,
                            onClick  = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon  = {
                                Icon(
                                    imageVector        = item.icon,
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Home.route,
            modifier         = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Home.route) {
                val words by viewModel.words.collectAsState()
                HomeScreen(
                    onCameraClick = { navController.navigate(Screen.Camera.route) },
                    wordCount     = words.size,
                )
            }

            composable(Screen.Camera.route) {
                CameraScreen(
                    onBack      = { navController.popBackStack() },
                    onWordAdded = { english, chinese -> viewModel.addWord(english, chinese) },
                )
            }

            composable(Screen.WordBook.route) {
                val words by viewModel.words.collectAsState()
                WordbookScreen(
                    words         = words,
                    onDayClick    = { date -> navController.navigate("wordbook/$date") },
                    onCameraClick = { navController.navigate(Screen.Camera.route) },
                )
            }

            composable(
                route     = Screen.WordBookDay.route,
                arguments = listOf(navArgument("date") { type = NavType.StringType }),
            ) { backStackEntry ->
                val date  = backStackEntry.arguments?.getString("date") ?: return@composable
                val words by viewModel.words.collectAsState()
                val dayWords = remember(words, date) {
                    words.filter { word ->
                        Instant.ofEpochMilli(word.createdAt)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .toString() == date
                    }
                }
                DayDetailScreen(
                    date            = date,
                    words           = dayWords,
                    onBack          = { navController.popBackStack() },
                    onUpdateChinese = { id, chinese -> viewModel.updateChinese(id, chinese) },
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
