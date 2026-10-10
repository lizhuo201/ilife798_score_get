package com.isuncy.ilife798.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.isuncy.ilife798.MainActivity
import com.isuncy.ilife798.R
import com.isuncy.ilife798.data.local.AccountStore
import com.isuncy.ilife798.data.local.HistoryStore
import com.isuncy.ilife798.data.model.HistoryRecord
import com.isuncy.ilife798.execution.ExecutionState
import com.isuncy.ilife798.execution.ScoreRunner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 前台服务：保活本次执行，完成后自动退出。 */
class ScoreService : Service() {

    companion object {
        const val ACTION_START = "com.isuncy.ilife798.action.START"
        const val ACTION_STOP = "com.isuncy.ilife798.action.STOP"
        private const val CHANNEL_ID = "score_exec"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, ScoreService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, ScoreService::class.java).setAction(ACTION_STOP))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var runJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "积分执行", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                ExecutionState.log("收到停止请求，正在结束...")
                ExecutionState.requestStop()
                runJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                if (runJob?.isActive != true) startRun()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startRun() {
        val accounts = AccountStore.load(this)
        startForegroundCompat("准备执行...")

        runJob = scope.launch {
            ExecutionState.reset()
            ExecutionState.setRunning(true)
            if (accounts.isEmpty()) ExecutionState.log("暂无账号，请先在首页添加或导入账号")
            val accountLogs = mutableListOf<String>()

            try {
                ScoreRunner().run(
                    accounts = accounts,
                    skipVerify = true,
                    onLog = { line ->
                        accountLogs += line
                        ExecutionState.log(line)
                    },
                    onAccountStart = { index, total, phone ->
                        accountLogs.clear()
                        ExecutionState.update {
                            it.copy(phone = phone, accountDone = index - 1, accountTotal = total)
                        }
                        notifyText("正在执行 " + index + "/" + total + "：" + phone)
                    },
                    onAccountDone = { result ->
                        ExecutionState.update {
                            it.copy(
                                successSends = it.successSends + result.successSends,
                                failedSends = it.failedSends + result.failedSends,
                                beforeScore = result.beforeScore,
                                afterScore = result.afterScore,
                                accountDone = it.accountDone + 1
                            )
                        }
                        HistoryStore.append(
                            this@ScoreService,
                            HistoryRecord(
                                time = System.currentTimeMillis(),
                                phone = result.phone,
                                successSends = result.successSends,
                                failedSends = result.failedSends,
                                beforeScore = result.beforeScore,
                                afterScore = result.afterScore,
                                logs = accountLogs.toList()
                            )
                        )
                    }
                )
            } catch (e: CancellationException) {
                ExecutionState.log("执行已停止")
                throw e
            } catch (e: Exception) {
                ExecutionState.log("执行异常：" + (e.message ?: "未知错误"))
            } finally {
                ExecutionState.setRunning(false)
                ExecutionState.log("本次执行结束")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun startForegroundCompat(text: String) {
        val notification = buildNotification(text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun notifyText(text: String) {
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("798积分助手")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .build()
    }
}
