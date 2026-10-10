package com.isuncy.ilife798.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    initialPhone: String,
    vm: LoginViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.initPhone(initialPhone) }
    LaunchedEffect(state.done) {
        if (state.done) navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialPhone.isEmpty()) "添加账号" else "重新登录") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.phone,
                onValueChange = { vm.setPhone(it) },
                label = { Text("手机号") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            // 第一步：图形验证码
            OutlinedTextField(
                value = state.captchaCode,
                onValueChange = { vm.setCaptchaCode(it) },
                label = { Text("图形验证码") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val request = remember(state.seed) {
                    ImageRequest.Builder(context)
                        .data(vm.captchaUrl())
                        .addHeader("user-agent", "Android_ilife798_3.1.7")
                        .addHeader("VersionCode", "31007")
                        .addHeader("ApplicationType", "1,1")
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = "图形验证码",
                    modifier = Modifier.size(width = 120.dp, height = 44.dp)
                )
                OutlinedButton(onClick = { vm.refreshCaptcha() }) { Text("换一张") }
            }
            Button(
                onClick = { vm.submitGraphic() },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.waitingSms) "重新发送短信验证码" else "获取短信验证码")
            }

            // 第二步：短信验证码
            OutlinedTextField(
                value = state.smsCode,
                onValueChange = { vm.setSmsCode(it) },
                label = { Text("短信验证码") },
                enabled = state.waitingSms,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { vm.submitSms() },
                enabled = state.waitingSms && !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("登录")
            }

            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            if (state.message.isNotEmpty()) {
                Text(state.message, color = MaterialTheme.colorScheme.tertiary)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
