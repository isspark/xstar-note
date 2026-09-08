package com.xstar.notebook.ui.settings

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xstar.notebook.data.settings.ThemeMode
import com.xstar.notebook.MinimalTodoWidgetProvider
import com.xstar.notebook.TodoWidgetProvider
import com.xstar.notebook.ui.appContainer
import com.xstar.notebook.ui.components.GradientTopBar

@Composable
fun SettingsScreen(onOpenDrawer: () -> Unit) {
    val settings = appContainer().settings
    val themeMode by settings.themeMode.collectAsState()
    val dynamicColor by settings.dynamicColor.collectAsState()
    val taskNotificationEnabled by settings.taskNotificationEnabled.collectAsState()
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionDenied = !granted
        settings.setTaskNotificationEnabled(granted)
    }

    fun updateTaskNotification(enabled: Boolean) {
        permissionDenied = false
        if (!enabled) {
            settings.setTaskNotificationEnabled(false)
        } else if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            settings.setTaskNotificationEnabled(true)
        }
    }

    fun pinTodoWidget(minimal: Boolean) {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return
        val providerClass = if (minimal) MinimalTodoWidgetProvider::class.java else TodoWidgetProvider::class.java
        val provider = ComponentName(context, providerClass)
        val successIntent = PendingIntent.getBroadcast(
            context,
            if (minimal) 50_002 else 50_001,
            Intent(context, providerClass).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.requestPinAppWidget(provider, null, successIntent)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GradientTopBar(title = "设置", subtitle = "工作台、任务提醒与配色", onMenu = onOpenDrawer)
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "工作台",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            ThemeOption(
                icon = Icons.Rounded.ViewCarousel,
                title = "统一工作台",
                hint = "启动后展示今日任务、仓库和快捷入口",
                selected = true,
                onClick = {},
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "任务提醒",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(
                            "通知栏展示待处理任务",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            if (permissionDenied) "通知权限未授予，请重新开启并允许通知"
                            else "持续展示任务摘要，并允许在锁屏页面显示",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (permissionDenied) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        )
                    }
                    Switch(
                        checked = taskNotificationEnabled,
                        onCheckedChange = ::updateTaskNotification,
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().clickable { pinTodoWidget(false) },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Widgets, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text("添加待办组件到桌面", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(
                            "在桌面新增、完成或恢复任务",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().clickable { pinTodoWidget(true) },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Widgets, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text("添加极简待办组件", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(
                            "全透明背景，仅保留列表与淡色轮廓",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "外观模式",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            ThemeOption(
                icon = Icons.Rounded.BrightnessAuto,
                title = "跟随系统",
                hint = "自动匹配系统的深浅色",
                selected = themeMode == ThemeMode.SYSTEM,
                onClick = { settings.setThemeMode(ThemeMode.SYSTEM) },
            )
            ThemeOption(
                icon = Icons.Rounded.LightMode,
                title = "浅色",
                hint = "明亮、活泼的纸面配色",
                selected = themeMode == ThemeMode.LIGHT,
                onClick = { settings.setThemeMode(ThemeMode.LIGHT) },
            )
            ThemeOption(
                icon = Icons.Rounded.DarkMode,
                title = "深色",
                hint = "夜间阅读更护眼",
                selected = themeMode == ThemeMode.DARK,
                onClick = { settings.setThemeMode(ThemeMode.DARK) },
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "配色",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(
                        Modifier.weight(1f).padding(start = 14.dp),
                    ) {
                        Text(
                            "动态取色（Material You）",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "Android 12+ 根据壁纸生成配色；关闭则使用 XStar 专属配色",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Switch(
                        checked = dynamicColor,
                        onCheckedChange = settings::setDynamicColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    icon: ImageVector,
    title: String,
    hint: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
            Column(
                Modifier.weight(1f).padding(start = 14.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}
