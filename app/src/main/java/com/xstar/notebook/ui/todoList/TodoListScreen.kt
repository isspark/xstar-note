package com.xstar.notebook.ui.todoList

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xstar.notebook.data.db.entity.TodoEntity
import com.xstar.notebook.ui.appContainer
import com.xstar.notebook.ui.components.EmptyState
import com.xstar.notebook.ui.components.GradientTopBar
import com.xstar.notebook.viewmodel.TodoListViewModel

private enum class TodoFilter(val label: String) {
    ALL("全部"),
    OPEN("待办"),
    DONE("已完成"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    repoId: Long,
    relPath: String,
    onBack: () -> Unit,
    vm: TodoListViewModel = viewModel {
        val app = this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY] as com.xstar.notebook.XstarApplication
        TodoListViewModel(repoId, relPath, app.container.repoRepository)
    },
) {
    val todos by vm.todos.collectAsState()
    val busy by vm.busy.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var filter by remember { mutableStateOf(TodoFilter.ALL) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it, duration = SnackbarDuration.Short)
            vm.consumeMessage()
        }
    }

    val doneCount = todos.count { it.checked }
    val total = todos.size
    val filtered = remember(todos, filter) {
        when (filter) {
            TodoFilter.ALL -> todos
            TodoFilter.OPEN -> todos.filter { !it.checked }
            TodoFilter.DONE -> todos.filter { it.checked }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GradientTopBar(
                title = "TODO 清单",
                subtitle = relPath.substringAfterLast('/'),
                onBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (todos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Rounded.Checklist,
                        title = "这份文档没有任务",
                        hint = "回到阅读页，用 - [ ] 或 - [x] 语法添加清单",
                    )
                }
                return@Column
            }

            ProgressCard(done = doneCount, total = total)
            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TodoFilter.entries.forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = {
                            Text(
                                when (f) {
                                    TodoFilter.ALL -> "全部 $total"
                                    TodoFilter.OPEN -> "待办 ${total - doneCount}"
                                    TodoFilter.DONE -> "已完成 $doneCount"
                                },
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(bottom = 160.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (filter == TodoFilter.DONE) "还没有完成的任务" else "太棒了，全部搞定",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filtered, key = { it.id }) { todo ->
                        TodoRow(
                            todo = todo,
                            enabled = !busy,
                            onToggle = { checked -> vm.toggle(todo, checked) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(done: Int, total: Int) {
    val fraction = if (total == 0) 0f else done.toFloat() / total
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.size(72.dp),
                    strokeWidth = 8.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                )
                Text(
                    "${(fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(Modifier.padding(start = 20.dp)) {
                Text(
                    "任务进度",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
                Text(
                    "已完成 $done / $total",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    if (total == done && total > 0) "全部完成，太棒了" else "坚持一点点，笔记会越来越多",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun TodoRow(todo: TodoEntity, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    val checked = todo.checked
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onToggle(!checked) },
        shape = MaterialTheme.shapes.medium,
        color = if (checked) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle(it) }, enabled = enabled)
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(
                    todo.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (checked) TextDecoration.LineThrough else null,
                    color = if (checked) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    todo.docRelPath,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
