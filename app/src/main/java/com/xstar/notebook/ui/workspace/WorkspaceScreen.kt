package com.xstar.notebook.ui.workspace

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xstar.notebook.XstarApplication
import com.xstar.notebook.data.db.entity.DocEntity
import com.xstar.notebook.data.db.entity.RepoEntity
import com.xstar.notebook.data.db.entity.TodoNodeEntity
import com.xstar.notebook.ui.components.GradientTopBar
import com.xstar.notebook.ui.theme.appGradientColors
import com.xstar.notebook.viewmodel.TodoHubViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private enum class WorkspaceFilter(val label: String) {
    ALL("全部"),
    TODAY("今天"),
    OVERDUE("已逾期"),
}

@Composable
fun WorkspaceScreen(
    onOpenDrawer: () -> Unit,
    onOpenTodoHub: () -> Unit,
    onOpenSearch: () -> Unit,
    onCreateTodo: () -> Unit,
    onOpenNote: (Long, String) -> Unit,
    onOpenRepos: () -> Unit,
    vm: TodoHubViewModel = viewModel {
        val app = this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY] as XstarApplication
        TodoHubViewModel(app.container.repoRepository, app.container.todoHubRepository)
    },
) {
    val state by vm.state.collectAsState()
    var now by remember { mutableStateOf(LocalTime.now()) }
    var filter by remember { mutableStateOf(WorkspaceFilter.ALL) }
    var showNewNote by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val allTasks = state.data.nodes
    val openTasks = allTasks.filter { !it.done }
    val todayCount = openTasks.count(::isDueToday)
    val overdueCount = openTasks.count(::isOverdue)
    val completedCount = allTasks.count { it.done }
    val filteredTasks = openTasks
        .filter {
            when (filter) {
                WorkspaceFilter.ALL -> true
                WorkspaceFilter.TODAY -> isDueToday(it)
                WorkspaceFilter.OVERDUE -> isOverdue(it)
            }
        }
        .sortedWith(compareBy<TodoNodeEntity> { it.dueAt ?: Long.MAX_VALUE }.thenByDescending { it.updatedAt })
        .take(6)

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(30_000)
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GradientTopBar(
                title = "工作台",
                subtitle = "聚焦今天，整理下一步",
                onMenu = onOpenDrawer,
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Rounded.Search, contentDescription = "搜索", tint = Color.White)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                FocusCard(
                    now = now,
                    openCount = openTasks.size,
                    todayCount = todayCount,
                    onCreateTodo = onCreateTodo,
                    onCreateNote = { showNewNote = true },
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MetricCard(
                        label = "待处理",
                        value = openTasks.size.toString(),
                        detail = if (todayCount > 0) "$todayCount 项今天到期" else "今天暂无到期",
                        icon = Icons.Rounded.TaskAlt,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(
                        label = "已完成",
                        value = completedCount.toString(),
                        detail = if (overdueCount > 0) "$overdueCount 项已逾期" else "没有逾期任务",
                        icon = Icons.Rounded.CheckCircle,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                SectionHeader(
                    title = "待处理任务",
                    action = "查看全部",
                    onClick = onOpenTodoHub,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    WorkspaceFilter.entries.forEach { option ->
                        FilterChip(
                            selected = filter == option,
                            onClick = { filter = option },
                            label = { Text(option.label) },
                            leadingIcon = if (filter == option) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                        )
                    }
                }
            }
            if (filteredTasks.isEmpty()) {
                item {
                    EmptyTasks(filter = filter, onCreateTodo = onCreateTodo)
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskCard(task = task, onToggle = { vm.toggle(task) })
                }
            }
            item {
                SectionHeader(
                    title = "最近更新笔记",
                    action = "查看仓库",
                    onClick = onOpenRepos,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
            if (state.recentDocs.isEmpty()) {
                item { EmptyRecentNotes(onCreateNote = { showNewNote = true }) }
            } else {
                items(state.recentDocs.take(5), key = { it.id }) { doc ->
                    RecentNoteCard(
                        doc = doc,
                        repoName = state.repos.firstOrNull { it.id == doc.repoId }?.displayName.orEmpty(),
                        onClick = { onOpenNote(doc.repoId, doc.relPath) },
                    )
                }
            }
        }
    }

    if (showNewNote) {
        NewNoteDialog(
            repos = state.repos,
            busy = state.busy,
            onDismiss = { if (!state.busy) showNewNote = false },
            onCreate = { repoId, title ->
                vm.createMarkdownNote(repoId, title) { createdRepoId, relPath ->
                    showNewNote = false
                    onOpenNote(createdRepoId, relPath)
                }
            },
        )
    }
}

@Composable
private fun FocusCard(
    now: LocalTime,
    openCount: Int,
    todayCount: Int,
    onCreateTodo: () -> Unit,
    onCreateNote: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val darkMode = isWorkspaceDarkMode()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .workspaceGradientBorder(shape, darkMode, emphasized = true),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (darkMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.74f)
            else MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = CircleShape,
                ) {
                    Icon(
                        Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(9.dp).size(20.dp),
                    )
                }
                Text(
                    formatToday(LocalDate.now()),
                    modifier = Modifier.padding(start = 10.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    now.format(DateTimeFormatter.ofPattern("HH:mm")),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                greeting(now),
                modifier = Modifier.padding(top = 18.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                when {
                    openCount == 0 -> "今天没有待处理事项，可以记录一个新想法。"
                    todayCount > 0 -> "$openCount 项待处理，其中 $todayCount 项计划今天完成。"
                    else -> "$openCount 项待处理，按自己的节奏逐项推进。"
                },
                modifier = Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickCreateButton(
                    label = "新建任务",
                    icon = Icons.Rounded.Add,
                    onClick = onCreateTodo,
                    primary = true,
                    modifier = Modifier.weight(1f),
                )
                QuickCreateButton(
                    label = "新建笔记",
                    icon = Icons.AutoMirrored.Rounded.NoteAdd,
                    onClick = onCreateNote,
                    primary = false,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun QuickCreateButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    primary: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                label,
                modifier = Modifier.padding(start = 6.dp),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun RecentNoteCard(
    doc: DocEntity,
    repoName: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val darkMode = isWorkspaceDarkMode()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .workspaceGradientBorder(shape, darkMode)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = workspaceCardColor(darkMode)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    Icons.Rounded.Description,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp).size(20.dp),
                )
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    doc.relPath.substringAfterLast('/').removeSuffix(".md").removeSuffix(".markdown"),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOf(repoName, doc.relPath.substringBeforeLast('/', "")).filter { it.isNotBlank() }.joinToString(" · "),
                    modifier = Modifier.padding(top = 3.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatUpdated(doc.modifiedAt),
                    modifier = Modifier.padding(top = 5.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun EmptyRecentNotes(onCreateNote: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val darkMode = isWorkspaceDarkMode()
    Surface(
        modifier = Modifier.fillMaxWidth().workspaceGradientBorder(shape, darkMode),
        color = if (darkMode) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = shape,
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onCreateNote).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Rounded.NoteAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("还没有最近笔记", fontWeight = FontWeight.SemiBold)
                Text(
                    "新建一篇笔记开始记录",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun NewNoteDialog(
    repos: List<RepoEntity>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onCreate: (Long, String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var selectedRepoId by remember(repos) { mutableStateOf(repos.firstOrNull()?.id) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建笔记") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (repos.isEmpty()) {
                    Text("请先在笔记管理中添加一个仓库。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("保存到", style = MaterialTheme.typography.labelLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        repos.forEach { repo ->
                            FilterChip(
                                selected = selectedRepoId == repo.id,
                                onClick = { selectedRepoId = repo.id },
                                label = { Text(repo.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("笔记标题") },
                        placeholder = { Text("例如：会议记录") },
                        singleLine = true,
                        enabled = !busy,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedRepoId?.let { onCreate(it, title) } },
                enabled = selectedRepoId != null && title.isNotBlank() && !busy,
            ) {
                Text(if (busy) "创建中..." else "创建并编辑")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("取消") }
        },
    )
}

private fun formatUpdated(epochMillis: Long): String {
    val dateTime = java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDateTime()
    val today = LocalDate.now()
    return when (dateTime.toLocalDate()) {
        today -> "今天 ${dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))} 更新"
        today.minusDays(1) -> "昨天更新"
        else -> dateTime.format(DateTimeFormatter.ofPattern("M月d日更新"))
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    detail: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val darkMode = isWorkspaceDarkMode()
    Card(
        modifier = modifier.workspaceGradientBorder(shape, darkMode),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = workspaceCardColor(darkMode)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                Text(
                    label,
                    modifier = Modifier.padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                value,
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                detail,
                modifier = Modifier.padding(top = 2.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(action, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun TaskCard(task: TodoNodeEntity, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val darkMode = isWorkspaceDarkMode()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .workspaceGradientBorder(shape, darkMode)
            .clickable(onClick = onToggle),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = workspaceCardColor(darkMode)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onToggle, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = "完成任务",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(Modifier.weight(1f).padding(start = 7.dp)) {
                Text(
                    task.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (task.note.isNotBlank()) {
                    Text(
                        task.note,
                        modifier = Modifier.padding(top = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = CircleShape,
                    ) {
                        Text(
                            "本地任务",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    task.dueAt?.let {
                        Text(
                            formatDue(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOverdue(task)) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun EmptyTasks(filter: WorkspaceFilter, onCreateTodo: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val darkMode = isWorkspaceDarkMode()
    Surface(
        modifier = Modifier.fillMaxWidth().workspaceGradientBorder(shape, darkMode),
        color = if (darkMode) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = shape,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(34.dp),
            )
            Text(
                when (filter) {
                    WorkspaceFilter.ALL -> "待办已清空"
                    WorkspaceFilter.TODAY -> "今天没有到期任务"
                    WorkspaceFilter.OVERDUE -> "没有逾期任务"
                },
                modifier = Modifier.padding(top = 10.dp),
                fontWeight = FontWeight.Bold,
            )
            if (filter == WorkspaceFilter.ALL) {
                Text(
                    "记录下一件要做的事",
                    modifier = Modifier.clickable(onClick = onCreateTodo).padding(top = 7.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

private fun greeting(now: LocalTime): String = when (now.hour) {
    in 5..10 -> "早上好，先处理这几件事"
    in 11..13 -> "中午好，看看接下来的安排"
    in 14..17 -> "下午好，继续推进今天的计划"
    else -> "晚上好，整理今天的收尾事项"
}

@Composable
private fun isWorkspaceDarkMode(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.35f

@Composable
private fun workspaceCardColor(darkMode: Boolean): Color =
    if (darkMode) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
    else MaterialTheme.colorScheme.surface

@Composable
private fun Modifier.workspaceGradientBorder(
    shape: Shape,
    enabled: Boolean,
    emphasized: Boolean = false,
): Modifier {
    if (!enabled) return this
    val alpha = if (emphasized) 0.78f else 0.48f
    val gradient = appGradientColors()
    return border(
        width = 1.dp,
        brush = Brush.linearGradient(
            listOf(
                gradient[0].copy(alpha = alpha),
                gradient[1].copy(alpha = alpha * 0.82f),
                gradient[2].copy(alpha = alpha * 0.58f),
            ),
        ),
        shape = shape,
    )
}

private fun formatToday(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA))

private fun formatDue(epochMillis: Long): String {
    val dateTime = java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDateTime()
    val today = LocalDate.now()
    return when (dateTime.toLocalDate()) {
        today -> "今天 ${dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        today.plusDays(1) -> "明天 ${dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        else -> dateTime.format(DateTimeFormatter.ofPattern("M/d HH:mm"))
    }
}

private fun isDueToday(task: TodoNodeEntity): Boolean =
    task.dueAt?.let {
        java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == LocalDate.now()
    } == true

private fun isOverdue(task: TodoNodeEntity): Boolean =
    task.dueAt?.let { it < System.currentTimeMillis() } == true
