package com.isuncy.ilife798.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.isuncy.ilife798.ui.history.HistoryScreen
import com.isuncy.ilife798.ui.home.HomeScreen
import com.isuncy.ilife798.ui.login.LoginScreen
import com.isuncy.ilife798.ui.run.RunLogScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_RUN = "run"
private const val ROUTE_HISTORY = "history"
const val ROUTE_LOGIN = "login?phone={phone}"

/** 新增账号：不带手机号，手机号输入框可编辑 */
const val ROUTE_LOGIN_NEW = "login?phone="

/** 重新登录：预填手机号（仍可编辑） */
fun loginRoute(phone: String): String = "login?phone=" + Uri.encode(phone)

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(ROUTE_HOME, "首页", Icons.Filled.Home),
    Tab(ROUTE_RUN, "执行", Icons.Filled.PlayArrow),
    Tab(ROUTE_HISTORY, "历史", Icons.Filled.List)
)

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != ROUTE_LOGIN) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(ROUTE_HOME) { HomeScreen(navController) }
            composable(
                ROUTE_LOGIN,
                arguments = listOf(navArgument("phone") { type = NavType.StringType; defaultValue = "" })
            ) { entry ->
                LoginScreen(
                    navController = navController,
                    initialPhone = entry.arguments?.getString("phone") ?: ""
                )
            }
            composable(ROUTE_RUN) { RunLogScreen(navController) }
            composable(ROUTE_HISTORY) { HistoryScreen(navController) }
        }
    }
}
