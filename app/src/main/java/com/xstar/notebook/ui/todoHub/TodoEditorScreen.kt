@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xstar.notebook.ui.todoHub

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xstar.notebook.data.todo.TodoPresets
import com.xstar.notebook.ui.appContainer
import com.xstar.notebook.ui.components.FilledActionButton
import com.xstar.notebook.ui.components.GradientTopBar
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@Composable
fun TodoEditorScreen(
    todoId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val repository = appContainer().todoHubRepository
    val data by remember(repository) { repository.observeData() }
        .collectAsState(initial = com.xstar.notebook.data.repo.TodoHubData())
    val existing = data.nodes.firstOrNull { it.id == todoId }
    val isNew = todoId == 0L
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var initialized by remember(todoId) { mutableStateOf(isNew) }
    var title by remember(todoId) { mutableStateOf("") }
    var note by remember(todoId) { mutableStateOf("") }
    var startAt by remember(todoId) { mutableStateOf<Long?>(null) }
    var dueAt by remember(todoId) { mutableStateOf<Long?>(null) }
    var tags by remember(todoId) { mutableStateOf<Set<Long>>(emptySet()) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(existing?.id) {
        if (!initialized && existing != null) {
            title = existing.title
            note = existing.note
            startAt = existing.startAt
            dueAt = existing.dueAt
            tags = data.links.filter { it.nodeId == existing.id }.mapTo(HashSet()) { it.categoryId }
            initialized = true
        }
    }

    fun chooseDate(current: Long?, defaultHour: Int, onPicked: (Long) -> Unit) {
        val calendar = Calendar.getInstance().apply {
            current?.let { timeInMillis = it }
            if (current == null) {
                set(Calendar.HOUR_OF_DAY, defaultHour)
                set(Calendar.MINUTE, 0)
            }
        }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendar.set(year, month, day, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), 0)
                calendar.set(Calendar.MILLISECOND, 0)
                onPicked(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    fun chooseTime(current: Long, onPicked: (Long) -> Unit) {
        val calendar = Calendar.getInstance().apply { timeInMillis = current }
        TimePickerDialog(
            context,
            { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                onPicked(calendar.timeInMillis)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true,
        ).show()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GradientTopBar(
                title = if (isNew) "新建 TODO" else "编辑 TODO",
                subtitle = if (isNew) "记录任务和时间安排" else "更新任务信息",
                onBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (!initialized) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                placeholder = { Text("输入 TODO 标题") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注") },
                placeholder = { Text("可选") },
                minLines = 3,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            FormSection("开始时间") {
                EditorDateTimeSelector(
                    value = startAt,
                    onDate = { chooseDate(startAt, 9) { startAt = it } },
                    onTime = { startAt?.let { value -> chooseTime(value) { startAt = it } } },
                    onClear = { startAt = null },
                )
            }
            FormSection("截止时间") {
                EditorDateTimeSelector(
                    value = dueAt,
                    onDate = { chooseDate(dueAt, 18) { dueAt = it } },
                    onTime = { dueAt?.let { value -> chooseTime(value) { dueAt = it } } },
                    onClear = { dueAt = null },
                )
                if (startAt != null && dueAt != null && dueAt!! < startAt!!) {
                    Text(
                        "截止时间不能早于开始时间",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            FormSection("分类") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.enabledSystems.forEach { system ->
                        Text(
                            system.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val categories = data.categoriesOf(system.id)
                            val categoryIds = categories.mapTo(HashSet()) { it.id }
                            categories.forEach { category ->
                                val selected = category.id in tags
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        tags = if (system.builtInKey == TodoPresets.KEY_TYPE) {
                                            val cleared = tags - categoryIds
                                            if (selected) cleared else cleared + category.id
                                        } else if (selected) {
                                            tags - category.id
                                        } else {
                                            tags + category.id
                                        }
                                    },
                                    label = { Text(category.name) },
                                    leadingIcon = {
                                        Box(
                                            Modifier.size(9.dp)
                                                .background(Color(category.colorArgb), CircleShape),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
            FilledActionButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && !busy &&
                    (startAt == null || dueAt == null || dueAt!! >= startAt!!),
                onClick = {
                    busy = true
                    scope.launch {
                        runCatching {
                            val id = if (isNew) {
                                repository.addNode(title, note, startAt, dueAt)
                            } else {
                                repository.updateNode(todoId, title, note, startAt, dueAt)
                                todoId
                            }
                            repository.setTags(id, tags)
                        }.onSuccess {
                            onSaved()
                        }.onFailure {
                            busy = false
                            snackbar.showSnackbar("保存失败：${it.message.orEmpty()}")
                        }
                    }
                },
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("保存", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FormSection(label: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun EditorDateTimeSelector(
    value: Long?,
    onDate: () -> Unit,
    onTime: () -> Unit,
    onClear: () -> Unit,
) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onDate,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.CalendarMonth, null, modifier = Modifier.size(18.dp))
                Text(
                    value?.let(::editorDate) ?: "选择日期",
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            if (value != null) {
                OutlinedButton(onClick = onTime, shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Rounded.Schedule, null, modifier = Modifier.size(18.dp))
                    Text(editorTime(value), modifier = Modifier.padding(start = 5.dp))
                }
            }
        }
        if (value != null) {
            TextButton(onClick = onClear) { Text("清除") }
        }
    }
}

private fun editorDate(millis: Long): String =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

private fun editorTime(millis: Long): String =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
