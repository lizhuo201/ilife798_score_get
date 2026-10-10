package com.isuncy.ilife798.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.isuncy.ilife798.execution.ExecutionState
import com.isuncy.ilife798.service.ScoreService
import com.isuncy.ilife798.ui.nav.ROUTE_LOGIN_NEW
import com.isuncy.ilife798.ui.nav.loginRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, vm: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    val accounts by vm.accounts.collectAsState()
    val running by ExecutionState.state.collectAsState()
    val importError by vm.importError.collectAsState()

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
        } catch (e: Exception) {
            ""
        }
        vm.importJson(text)
    }

    importError?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.clearImportError() },
            confirmButton = { TextButton(onClick = { vm.clearImportError() }) { Text("知道了") } },
            title = { Text("导入") },
            text = { Text(msg) }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("798积分助手") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "本工具仅用于自动完成每日积分任务，使用前请自行评估封号风险。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { navController.navigate(ROUTE_LOGIN_NEW) }) { Text("添加账号") }
                OutlinedButton(onClick = {
                    importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                }) { Text("导入凭证") }
                Spacer(Modifier.weight(1f))
                if (running.running) {
                    OutlinedButton(onClick = { ScoreService.stop(context) }) { Text("停止") }
                } else {
                    Button(onClick = { ScoreService.start(context) }) { Text("一键执行") }
                }
            }
            Spacer(Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 112.dp)) {
                items(accounts, key = { it.account.phone }) { item ->
                    AccountCard(
                        item = item,
                        onRelogin = { navController.navigate(loginRoute(item.account.phone)) },
                        onRemove = { vm.remove(item.account.phone) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountCard(
    item: AccountItem,
    onRelogin: () -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.account.phone,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                when (item.valid) {
                    null -> {
                        CircularProgressIndicator(modifier = Modifier.height(16.dp))
                        Spacer(Modifier.height(8.dp))
                    }
                    true -> Text("有效", color = MaterialTheme.colorScheme.secondary)
                    false -> Text("无效", color = MaterialTheme.colorScheme.error)
                }
            }
            Text(
                if (item.score != null) "当前积分：" + item.score else "积分获取中...",
                style = MaterialTheme.typography.bodyMedium
            )
            Row {
                TextButton(onClick = onRelogin) { Text("重新登录") }
                TextButton(onClick = onRemove) { Text("删除") }
            }
        }
    }
}
