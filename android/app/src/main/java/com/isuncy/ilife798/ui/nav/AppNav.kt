package com.isuncy.ilife798.ui.nav

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.isuncy.ilife798.core.AgreementStore
import com.isuncy.ilife798.ui.disclaimer.DisclaimerGate
import com.isuncy.ilife798.ui.history.HistoryScreen
import com.isuncy.ilife798.ui.home.HomeScreen
import com.isuncy.ilife798.ui.login.LoginScreen
import com.isuncy.ilife798.ui.run.RunLogScreen
import com.isuncy.ilife798.ui.theme.Ilife798Background
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

private const val ROUTE_HOME = "home"
private const val ROUTE_RUN = "run"
private const val ROUTE_HISTORY = "history"
const val ROUTE_LOGIN = "login?phone={phone}"

/** 新增账号：不带手机号，手机号输入框可编辑 */
const val ROUTE_LOGIN_NEW = "login?phone="

/** 重新登录：预填手机号（仍可编辑） */
fun loginRoute(phone: String): String = "login?phone=" + Uri.encode(phone)

private val tabs = listOf(
    NavTab(ROUTE_HOME, "首页", Icons.Filled.Home),
    NavTab(ROUTE_RUN, "执行", Icons.Filled.PlayArrow),
    NavTab(ROUTE_HISTORY, "历史", Icons.Filled.List)
)

@Composable
fun AppNav() {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    var agreed by remember { mutableStateOf(AgreementStore.isAgreed(context)) }

    // 首启免责声明门：不同意直接退出
    if (!agreed) {
        DisclaimerGate(
            onAgree = {
                AgreementStore.setAgreed(context)
                agreed = true
            },
            onDisagree = { (context as? Activity)?.finish() }
        )
        return
    }

    // 同意后（而非启动瞬间）再申请通知权限，保证声明最先出现
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val backdrop = rememberLayerBackdrop()
    val selectedIndex = tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    Box(modifier = Modifier.fillMaxSize().background(Ilife798Background)) {
        // 玻璃胶囊下方的内容层：整层录制进 backdrop，供玻璃采样模糊/折射
        Box(modifier = Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            NavHost(
                navController = navController,
                startDestination = ROUTE_HOME,
                modifier = Modifier.fillMaxSize()
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

        if (currentRoute != ROUTE_LOGIN) {
            CapsuleBottomTabs(
                tabs = tabs,
                selectedIndex = selectedIndex,
                onSelect = { index ->
                    navController.navigate(tabs[index].route) {
                        popUpTo(ROUTE_HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                backdrop = backdrop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxWidth()
            )
        }
    }
}
