package com.kifpool.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kifpool.app.WalletViewModel
import com.kifpool.app.ui.screens.AddEditScreen
import com.kifpool.app.ui.screens.CategoriesScreen
import com.kifpool.app.ui.screens.DebtsScreen
import com.kifpool.app.ui.screens.HomeScreen
import com.kifpool.app.ui.screens.LockScreen
import com.kifpool.app.ui.screens.RemindersScreen
import com.kifpool.app.ui.screens.SettingsScreen
import com.kifpool.app.ui.screens.StatsScreen

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AppRoot(
    vm: WalletViewModel,
    locked: Boolean = false,
    onUnlock: () -> Unit = {},
    openReminders: Boolean = false,
    onRemindersOpened: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    // ---- دروازه قفل: تا باز نشود، هیچ صفحه‌ای دیده نمی‌شود -------------------
    if (locked) {
        LockScreen(
            vm = vm,
            onUnlock = onUnlock
        )
        return
    }

    // کلیک روی اعلان یادآور => باز شدن مستقیم صفحه یادآورها
    LaunchedEffect(openReminders) {
        if (openReminders) {
            try {
                navController.navigate("reminders") { launchSingleTop = true }
            } catch (e: Exception) {
                // ناوبری هرگز نباید برنامه را ببندد
            }
            onRemindersOpened()
        }
    }

    val tabs = remember {
        listOf(
            TabItem("home", "خانه", Icons.Rounded.Home),
            TabItem("stats", "آمار", Icons.Rounded.BarChart),
            TabItem("debts", "دفتر", Icons.Rounded.Handshake),
            TabItem("categories", "دسته‌ها", Icons.Rounded.Category),
            TabItem("settings", "تنظیمات", Icons.Rounded.Settings)
        )
    }
    val isTab = tabs.any { it.route == route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (isTab) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    tabs.forEach { tab ->
                        val selected = route == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (isTab) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("add") },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("ثبت تراکنش") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(vm, onEdit = { id -> navController.navigate("edit/$id") })
            }
            composable("stats") { StatsScreen(vm) }
            composable("debts") { DebtsScreen(vm) }
            composable("categories") { CategoriesScreen(vm) }
            composable("settings") {
                SettingsScreen(
                    vm,
                    onOpenReminders = { navController.navigate("reminders") { launchSingleTop = true } }
                )
            }
            composable("reminders") {
                RemindersScreen(vm, onBack = { navController.popBackStack() })
            }
            composable("add") {
                AddEditScreen(vm, txId = null, onBack = { navController.popBackStack() })
            }
            composable(
                route = "edit/{txId}",
                arguments = listOf(navArgument("txId") { type = NavType.LongType })
            ) { entry ->
                val txId = entry.arguments?.getLong("txId") ?: 0L
                AddEditScreen(vm, txId = txId, onBack = { navController.popBackStack() })
            }
        }
    }
}
