package com.xstar.notebook

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TodoWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { update(context, manager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_TOGGLE) return
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as XstarApplication
                val item = app.container.database.todoNodeDao().getById(taskId)
                if (item != null) app.container.todoHubRepository.toggle(taskId, !item.done)
                updateAll(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.xstar.notebook.widget.TOGGLE"
        const val EXTRA_TASK_ID = "task_id"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, TodoWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            ids.forEach { update(context, manager, it) }
            manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_task_list)
        }

        private fun update(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val serviceIntent = Intent(context, TodoWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            val addIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_CREATE_TODO, true)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val toggleIntent = Intent(context, TodoWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE
            }
            val views = RemoteViews(context.packageName, R.layout.widget_todo).apply {
                setRemoteAdapter(R.id.widget_task_list, serviceIntent)
                setEmptyView(R.id.widget_task_list, R.id.widget_empty)
                setOnClickPendingIntent(
                    R.id.widget_add,
                    PendingIntent.getActivity(
                        context,
                        30_000 + widgetId,
                        addIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
                setPendingIntentTemplate(
                    R.id.widget_task_list,
                    PendingIntent.getBroadcast(
                        context,
                        40_000 + widgetId,
                        toggleIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                    ),
                )
            }
            manager.updateAppWidget(widgetId, views)
        }
    }
}
