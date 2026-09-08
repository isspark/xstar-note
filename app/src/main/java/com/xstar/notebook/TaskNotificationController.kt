package com.xstar.notebook

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.xstar.notebook.data.db.entity.TodoNodeEntity
import com.xstar.notebook.data.repo.TodoHubRepository
import com.xstar.notebook.data.settings.AppSettings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class TaskNotificationController(
    private val context: Context,
    private val settings: AppSettings,
    private val todoRepository: TodoHubRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        createChannel()
        scope.launch {
            combine(settings.taskNotificationEnabled, todoRepository.observeData()) { enabled, data ->
                enabled to data.nodes.filter { !it.done }
            }.collect { (enabled, tasks) ->
                TodoWidgetProvider.updateAll(context)
                if (enabled && canPostNotifications() && tasks.isNotEmpty()) {
                    post(tasks)
                } else {
                    NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
                }
            }
        }
    }

    private fun createChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "待处理任务",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "在通知栏和锁屏显示待处理任务摘要"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
            },
        )
    }

    private fun canPostNotifications(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    private fun post(tasks: List<TodoNodeEntity>) {
        val sorted = tasks.sortedWith(
            compareBy<TodoNodeEntity> { it.dueAt ?: Long.MAX_VALUE }.thenByDescending { it.updatedAt },
        )
        val todayCount = sorted.count(::isDueToday)
        val intent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val style = NotificationCompat.InboxStyle()
            .setBigContentTitle("${tasks.size} 项待处理")
            .setSummaryText(if (todayCount > 0) "$todayCount 项今天到期" else "XStar 笔记")
        sorted.take(MAX_LINES).forEach { task ->
            style.addLine(buildString {
                append(task.title)
                task.dueAt?.let { append("  ·  ").append(formatDue(it)) }
            })
        }
        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${tasks.size} 项待处理")
            .setContentText(if (todayCount > 0) "$todayCount 项今天到期" else sorted.first().title)
            .setContentIntent(intent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        NotificationManagerCompat.from(context).notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("${tasks.size} 项待处理")
                .setContentText(if (todayCount > 0) "$todayCount 项今天到期" else sorted.first().title)
                .setStyle(style)
                .setContentIntent(intent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setOngoing(true)
                .setPublicVersion(publicVersion)
                .build(),
        )
    }

    private fun isDueToday(task: TodoNodeEntity): Boolean = task.dueAt?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now()
    } == true

    private fun formatDue(epochMillis: Long): String {
        val time = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
        return if (time.toLocalDate() == LocalDate.now()) {
            "今天 ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        } else {
            time.format(DateTimeFormatter.ofPattern("M/d HH:mm"))
        }
    }

    private companion object {
        const val CHANNEL_ID = "pending_tasks_lockscreen"
        const val NOTIFICATION_ID = 1001
        const val MAX_LINES = 5
    }
}
