package com.xstar.notebook

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class MinimalTodoWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { update(context, manager, it) }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, MinimalTodoWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            ids.forEach { update(context, manager, it) }
            manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_task_list)
        }

        private fun update(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val serviceIntent = Intent(context, TodoWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(TodoWidgetService.EXTRA_MINIMAL, true)
                data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            val addIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_CREATE_TODO, true)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val toggleIntent = Intent(context, TodoWidgetProvider::class.java).apply {
                action = TodoWidgetProvider.ACTION_TOGGLE
            }
            val views = RemoteViews(context.packageName, R.layout.widget_todo_minimal).apply {
                setRemoteAdapter(R.id.widget_task_list, serviceIntent)
                setEmptyView(R.id.widget_task_list, R.id.widget_empty)
                setOnClickPendingIntent(
                    R.id.widget_add,
                    PendingIntent.getActivity(
                        context,
                        60_000 + widgetId,
                        addIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
                setPendingIntentTemplate(
                    R.id.widget_task_list,
                    PendingIntent.getBroadcast(
                        context,
                        70_000 + widgetId,
                        toggleIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                    ),
                )
            }
            manager.updateAppWidget(widgetId, views)
        }
    }
}
