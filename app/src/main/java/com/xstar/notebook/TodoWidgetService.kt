package com.xstar.notebook

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.content.ContextCompat
import com.xstar.notebook.data.db.entity.TodoNodeEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.runBlocking

class TodoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        TaskFactory(applicationContext, intent.getBooleanExtra(EXTRA_MINIMAL, false))

    private class TaskFactory(
        private val context: Context,
        private val minimal: Boolean,
    ) : RemoteViewsFactory {
        private var tasks = emptyList<TodoNodeEntity>()

        override fun onCreate() = Unit

        override fun onDataSetChanged() {
            val app = context.applicationContext as XstarApplication
            tasks = runBlocking {
                app.container.database.todoNodeDao().getAll()
                    .sortedWith(compareBy<TodoNodeEntity> { it.done }.thenBy { it.dueAt ?: Long.MAX_VALUE })
                    .take(20)
            }
        }

        override fun onDestroy() = Unit
        override fun getCount(): Int = tasks.size

        override fun getViewAt(position: Int): RemoteViews? {
            val task = tasks.getOrNull(position) ?: return null
            return RemoteViews(
                context.packageName,
                if (minimal) R.layout.widget_todo_minimal_item else R.layout.widget_todo_item,
            ).apply {
                setTextViewText(R.id.widget_task_check, if (task.done) "✓" else "○")
                setTextViewText(R.id.widget_task_title, task.title)
                setTextViewText(R.id.widget_task_due, task.dueAt?.let(::formatDue).orEmpty())
                setTextColor(
                    R.id.widget_task_title,
                    ContextCompat.getColor(context, if (task.done) R.color.widget_muted else R.color.widget_text),
                )
                setOnClickFillInIntent(
                    R.id.widget_task_check,
                    Intent().putExtra(TodoWidgetProvider.EXTRA_TASK_ID, task.id),
                )
            }
        }

        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount(): Int = 1
        override fun getItemId(position: Int): Long = tasks.getOrNull(position)?.id ?: position.toLong()
        override fun hasStableIds(): Boolean = true

        private fun formatDue(epochMillis: Long): String {
            val time = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
            return if (time.toLocalDate() == LocalDate.now()) {
                "今天 ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            } else {
                time.format(DateTimeFormatter.ofPattern("M/d HH:mm"))
            }
        }
    }

    companion object {
        const val EXTRA_MINIMAL = "minimal"
    }
}
