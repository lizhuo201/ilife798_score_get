package com.isuncy.ilife798.ui.run

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.isuncy.ilife798.execution.ExecutionState
import com.isuncy.ilife798.service.ScoreService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunLogScreen(navController: NavController) {
    val state by ExecutionState.state.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(state.logs.size) {
        if (state.logs.isNotEmpty()) listState.animateScrollToItem(state.logs.size - 1)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("执行日志") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (state.running || state.logs.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    if (state.running) {
                        Text(
                            "正在执行：" + state.phone + "（" + state.accountDone + "/" + state.accountTotal + "）",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "成功 " + state.successSends + " / 失败 " + state.failedSends,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Button(
                        onClick = { ScoreService.stop(context) },
                        enabled = state.running,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.running) "停止执行" else "执行已结束")
                    }
                }
            }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                itemsIndexed(state.logs) { index, line ->
                    Text(
                        line,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
