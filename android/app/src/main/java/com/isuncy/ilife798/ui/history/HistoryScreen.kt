package com.isuncy.ilife798.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.isuncy.ilife798.data.model.HistoryRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController, vm: HistoryViewModel = viewModel()) {
    val records by vm.records.collectAsState()
    val expanded = remember { mutableStateMapOf<Long, Boolean>() }
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    // 每次进入页面刷新（执行完成后回来能看到新记录）
    LaunchedEffect(Unit) { vm.reload() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("历史记录") },
                actions = {
                    TextButton(onClick = { vm.clear() }) { Text("清空") }
                }
            )
        }
    ) { padding ->
        if (records.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text("暂无执行记录")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(records) { record ->
                    RecordCard(
                        record = record,
                        expanded = expanded[record.time] == true,
                        dateFormat = dateFormat,
                        onToggle = { expanded[record.time] = expanded[record.time] != true }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordCard(
    record: HistoryRecord,
    expanded: Boolean,
    dateFormat: SimpleDateFormat,
    onToggle: () -> Unit
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable { onToggle() }) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    record.phone,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    dateFormat.format(Date(record.time)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                "成功 " + record.successSends + " / 失败 " + record.failedSends +
                    "，积分 " + record.beforeScore + " -> " + record.afterScore,
                style = MaterialTheme.typography.bodyMedium
            )
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                record.logs.forEach { line ->
                    Text(
                        line,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}
