@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xstar.notebook.ui.todoHub

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xstar.notebook.data.db.entity.CategoryEntity
import com.xstar.notebook.data.db.entity.RepoEntity
import com.xstar.notebook.data.db.entity.TodoNodeEntity
import com.xstar.notebook.data.repo.TodoHubData
import com.xstar.notebook.data.todo.TodoPresets
import com.xstar.notebook.ui.components.EmptyState
import com.xstar.notebook.ui.components.GradientTopBar
import com.xstar.notebook.ui.components.XStarFab
import com.xstar.notebook.viewmodel.TodoDoneFilter
import com.xstar.notebook.viewmodel.TodoGroup
import com.xstar.notebook.viewmodel.TodoHubUiState
import com.xstar.notebook.viewmodel.TodoHubViewModel
import com.xstar.notebook.viewmodel.TodoRow
import com.xstar.notebook.viewmodel.TodoViewMode
import com.xstar.notebook.viewmodel.TodoViews
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

private enum class CalendarListMode { DATE, OVERDUE, UNDATED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoHubScreen(
    onOpenDrawer: () -> Unit,
    onOpenClassify: () -> Unit,
    onCreateTodo: () -> Unit,
    onEditTodo: (Long) -> Unit,
    vm: TodoHubViewModel = viewModel {
        val app = this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY] as com.xstar.notebook.XstarApplication
        TodoHubViewModel(app.container.repoRepository, app.container.todoHubRepository)
    },
) {
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var deleting by remember { mutableStateOf<TodoNodeEntity?>(null) }
    var showFilter by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var topMenu by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it, duration = SnackbarDuration.Short)
            vm.consumeMessage()
        }
    }
    LaunchedEffect(showImport, state.selectedRepoId) {
        if (showImport) state.selectedRepoId?.let(vm::refreshFiles)
    }

    val (openCount, doneCount) = TodoViews.summary(state.data)
    val groups = remember(
        state.data,
        state.selectedSystemId,
        state.selectedCategoryId,
        state.doneFilter,
    ) {
        TodoViews.groups(
            state.data,
            state.activeSystem?.id,
            state.selectedCategoryId,
            state.doneFilter,
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GradientTopBar(
                title = "TODO",
                subtitle = "$openCount 项待办 · $doneCount 已完成",
                onMenu = onOpenDrawer,
                actions = {
                    IconButton(onClick = {
                        vm.setViewMode(
                            if (state.viewMode == TodoViewMode.CATEGORY) TodoViewMode.CALENDAR
                            else TodoViewMode.CATEGORY,
                        )
                    }) {
                        Icon(
                            if (state.viewMode == TodoViewMode.CATEGORY) Icons.Rounded.CalendarMonth
                            else Icons.Rounded.ViewAgenda,
                            if (state.viewMode == TodoViewMode.CATEGORY) "切换到日历" else "切换到分类",
                            tint = Color.White,
                        )
                    }
                    IconButton(onClick = onOpenClassify) {
                        Icon(Icons.Rounded.Tune, "分类管理", tint = Color.White)
                    }
                    Box {
                        IconButton(onClick = { topMenu = true }) {
                            Icon(Icons.Rounded.MoreVert, "更多", tint = Color.White)
                        }
                        DropdownMenu(expanded = topMenu, onDismissRequest = { topMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("导出到仓库") },
                                leadingIcon = { Icon(Icons.Rounded.CloudUpload, null) },
                                onClick = {
                                    topMenu = false
                                    showExport = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("从仓库导入") },
                                leadingIcon = { Icon(Icons.Rounded.CloudDownload, null) },
                                onClick = {
                                    topMenu = false
                                    showImport = true
                                },
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                XStarFab(
                    onClick = { showFilter = true },
                    icon = Icons.Rounded.FilterList,
                    contentDescription = "筛选",
                )
                XStarFab(
                    onClick = onCreateTodo,
                    icon = Icons.Rounded.Add,
                    contentDescription = "新建 TODO",
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state.viewMode) {
                TodoViewMode.CATEGORY -> CategoryView(
                    state = state,
                    groups = groups,
                    onToggle = vm::toggle,
                    onEdit = { onEditTodo(it.id) },
                    onDelete = { deleting = it },
                )
                TodoViewMode.CALENDAR -> CalendarView(
                    state = state,
                    onToggle = vm::toggle,
                    onEdit = { onEditTodo(it.id) },
                    onDelete = { deleting = it },
                )
            }
        }
    }

    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除 TODO", fontWeight = FontWeight.Bold) },
            text = { Text("确定删除「${item.title}」吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    vm.delete(item)
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } },
        )
    }
    if (showFilter) {
        FilterDialog(
            state = state,
            onDismiss = { showFilter = false },
            onApply = { systemId, categoryId, filter ->
                showFilter = false
                vm.applyFilter(systemId, categoryId, filter)
            },
        )
    }
    if (showExport) {
        ExportDialog(
            repos = state.repos,
            initialRepoId = state.selectedRepoId,
            busy = state.busy,
            onDismiss = { showExport = false },
            onExport = { repoId, folder ->
                showExport = false
                vm.exportToRepo(repoId, folder)
            },
        )
    }
    if (showImport) {
        ImportDialog(
            repos = state.repos,
            repoFiles = state.repoFiles,
            selectedRepoId = state.selectedRepoId,
            busy = state.busy,
            onSelectRepo = vm::selectRepo,
            onDismiss = { showImport = false },
            onImport = { repoId, path ->
                showImport = false
                vm.importFile(repoId, path)
            },
        )
    }
}

@Composable
private fun CategoryView(
    state: TodoHubUiState,
    groups: List<TodoGroup>,
    onToggle: (TodoNodeEntity) -> Unit,
    onEdit: (TodoNodeEntity) -> Unit,
    onDelete: (TodoNodeEntity) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        val visibleGroups = groups.filter { it.rows.isNotEmpty() || state.selectedCategoryId != null }
        if (visibleGroups.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(Icons.Rounded.Checklist, "没有匹配的 TODO", "切换分类或完成状态再看看")
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(visibleGroups, key = { it.category.id }) { group ->
                    CategoryGroupCard(group, onToggle, onEdit, onDelete)
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }
    }
}

@Composable
private fun FilterDialog(
    state: TodoHubUiState,
    onDismiss: () -> Unit,
    onApply: (Long?, Long?, TodoDoneFilter) -> Unit,
) {
    var systemId by remember { mutableStateOf(state.activeSystem?.id) }
    var categoryId by remember { mutableStateOf(state.selectedCategoryId) }
    var filter by remember { mutableStateOf(state.doneFilter) }
    val categories = state.data.categoriesOf(systemId ?: -1L)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("筛选 TODO", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("完成状态", style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TodoDoneFilter.entries.forEach { option ->
                        FilterChip(
                            selected = filter == option,
                            onClick = { filter = option },
                            label = { Text(option.label) },
                        )
                    }
                }
                Text("分类系统", style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.enabledSystems.forEach { system ->
                        FilterChip(
                            selected = systemId == system.id,
                            onClick = {
                                systemId = system.id
                                categoryId = null
                            },
                            label = { Text(system.name) },
                        )
                    }
                }
                Text("分类（可选）", style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = categoryId == null,
                        onClick = { categoryId = null },
                        label = { Text("全部分类") },
                    )
                    categories.forEach { category ->
                        FilterChip(
                            selected = categoryId == category.id,
                            onClick = { categoryId = category.id },
                            label = { Text(category.name) },
                            leadingIcon = {
                                Box(Modifier.size(9.dp).background(catColor(category), CircleShape))
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(systemId, categoryId, filter) }) {
                Text("应用", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun CategoryGroupCard(
    group: TodoGroup,
    onToggle: (TodoNodeEntity) -> Unit,
    onEdit: (TodoNodeEntity) -> Unit,
    onDelete: (TodoNodeEntity) -> Unit,
) {
    val color = catColor(group.category)
    var collapsed by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.08f)) {
        Column {
            Row(
                Modifier.fillMaxWidth().background(color).clickable { collapsed = !collapsed }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(group.category.name, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("${group.rows.size}", color = Color.White.copy(alpha = 0.8f))
            }
            if (!collapsed) {
                if (group.rows.isEmpty()) {
                    Text(
                        "暂无 TODO",
                        color = color,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp),
                    )
                } else {
                    group.rows.forEachIndexed { index, row ->
                        TodoLine(row, onToggle, onEdit, onDelete)
                        if (index < group.rows.lastIndex) {
                            HorizontalDivider(color = color.copy(alpha = 0.12f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarView(
    state: TodoHubUiState,
    onToggle: (TodoNodeEntity) -> Unit,
    onEdit: (TodoNodeEntity) -> Unit,
    onDelete: (TodoNodeEntity) -> Unit,
) {
    val data = state.data
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var listMode by remember { mutableStateOf(CalendarListMode.DATE) }
    val dateColors = remember(
        data,
        month,
        state.selectedSystemId,
        state.selectedCategoryId,
        state.doneFilter,
    ) {
        TodoViews.dateColors(
            data,
            month,
            state.activeSystem?.id,
            state.selectedCategoryId,
            state.doneFilter,
        )
    }
    val rows = remember(
        data,
        selectedDate,
        listMode,
        state.selectedSystemId,
        state.selectedCategoryId,
        state.doneFilter,
    ) {
        when (listMode) {
            CalendarListMode.DATE -> TodoViews.rowsForDate(
                data,
                selectedDate,
                state.activeSystem?.id,
                state.selectedCategoryId,
                state.doneFilter,
            )
            CalendarListMode.OVERDUE -> TodoViews.overdueRows(
                data,
                state.activeSystem?.id,
                state.selectedCategoryId,
            )
            CalendarListMode.UNDATED -> TodoViews.undatedRows(
                data,
                state.activeSystem?.id,
                state.selectedCategoryId,
                state.doneFilter,
            )
        }
    }

    Column(Modifier.fillMaxSize()) {
        MonthCalendar(
            month = month,
            selectedDate = selectedDate,
            colors = dateColors,
            onPrevious = {
                month = month.minusMonths(1)
                selectedDate = month.atDay(1)
                listMode = CalendarListMode.DATE
            },
            onNext = {
                month = month.plusMonths(1)
                selectedDate = month.atDay(1)
                listMode = CalendarListMode.DATE
            },
            onSelect = {
                selectedDate = it
                listMode = CalendarListMode.DATE
            },
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = listMode == CalendarListMode.DATE,
                onClick = { listMode = CalendarListMode.DATE },
                label = { Text(selectedDate.format(DateTimeFormatter.ofPattern("MM月dd日"))) },
            )
            FilterChip(
                selected = listMode == CalendarListMode.OVERDUE,
                onClick = { listMode = CalendarListMode.OVERDUE },
                label = { Text("已过期") },
            )
            FilterChip(
                selected = listMode == CalendarListMode.UNDATED,
                onClick = { listMode = CalendarListMode.UNDATED },
                label = { Text("未设置日期") },
            )
        }
        if (rows.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("这个视图没有 TODO", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            ) {
                items(rows, key = { it.node.id }) { row ->
                    TodoLine(row, onToggle, onEdit, onDelete)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    selectedDate: LocalDate,
    colors: Map<LocalDate, List<Long>>,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value - 1
    val cells = List(leading) { null } + (1..month.lengthOfMonth()).map(month::atDay)
    val weeks = cells.chunked(7)
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "上个月")
                }
                Text(
                    month.format(DateTimeFormatter.ofPattern("yyyy年 MM月")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onNext) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, "下个月")
                }
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            weeks.forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    (week + List(7 - week.size) { null }).forEach { date ->
                        Box(
                            Modifier.weight(1f).height(42.dp)
                                .clickable(enabled = date != null) { date?.let(onSelect) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (date != null) {
                                val selected = date == selectedDate
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        Modifier.size(28.dp).background(
                                            if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            CircleShape,
                                        ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            date.dayOfMonth.toString(),
                                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    val dots = colors[date].orEmpty()
                                    if (dots.isNotEmpty()) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            dots.forEach { color ->
                                                Box(Modifier.size(5.dp).background(Color(color), CircleShape))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoLine(
    row: TodoRow,
    onToggle: (TodoNodeEntity) -> Unit,
    onEdit: (TodoNodeEntity) -> Unit,
    onDelete: (TodoNodeEntity) -> Unit,
) {
    val item = row.node
    val overdue = TodoViews.isOverdue(item)
    val typeTag = row.tags.firstOrNull { it.systemId == TodoPresets.SYSTEM_ID_TYPE }
    var menu by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth().clickable { onToggle(item) }.padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = item.done, onCheckedChange = { onToggle(item) })
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                    color = if (item.done) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                typeTag?.let { TypePill(it) }
            }
            if (item.startAt != null || item.dueAt != null || item.note.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val schedule = formatSchedule(item.startAt, item.dueAt)
                    if (schedule != null) {
                        Icon(
                            Icons.Rounded.Schedule,
                            null,
                            tint = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            if (overdue) "已过期 · $schedule" else schedule,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                    if (item.note.isNotBlank()) {
                        Text(
                            item.note,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(
                                start = if (item.startAt == null && item.dueAt == null) 0.dp else 8.dp,
                            ),
                        )
                    }
                }
            }
        }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.MoreVert, "更多", tint = MaterialTheme.colorScheme.outline)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("编辑") },
                    leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                    onClick = {
                        menu = false
                        onEdit(item)
                    },
                )
                DropdownMenuItem(
                    text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menu = false
                        onDelete(item)
                    },
                )
            }
        }
    }
}

@Composable
private fun TypePill(category: CategoryEntity) {
    val color = catColor(category)
    Surface(color = color.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
        Text(
            category.name,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

private fun catColor(category: CategoryEntity): Color = Color(category.colorArgb)

private fun formatSchedule(startAt: Long?, dueAt: Long?): String? = when {
    startAt != null && dueAt != null -> "${formatDue(startAt)} → ${formatDue(dueAt)}"
    startAt != null -> "开始 ${formatDue(startAt)}"
    dueAt != null -> "截止 ${formatDue(dueAt)}"
    else -> null
}

private fun formatDue(millis: Long): String =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))

@Composable
private fun ExportDialog(
    repos: List<RepoEntity>,
    initialRepoId: Long?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onExport: (Long, String) -> Unit,
) {
    var repoId by remember { mutableStateOf(initialRepoId) }
    var folder by remember { mutableStateOf("todo") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出到仓库", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (repos.isEmpty()) {
                    Text("还没有仓库，请先在笔记管理中添加。")
                } else {
                    RepoSelectList(repos, repoId) { repoId = it }
                    OutlinedTextField(
                        value = folder,
                        onValueChange = { folder = it },
                        label = { Text("导出目录（留空为根目录）") },
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = repoId != null && !busy,
                onClick = { repoId?.let { onExport(it, folder) } },
            ) { Text("导出并提交") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ImportDialog(
    repos: List<RepoEntity>,
    repoFiles: List<String>,
    selectedRepoId: Long?,
    busy: Boolean,
    onSelectRepo: (Long) -> Unit,
    onDismiss: () -> Unit,
    onImport: (Long, String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("从仓库导入", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (repos.isEmpty()) {
                    Text("还没有仓库，请先在笔记管理中添加。")
                } else {
                    RepoSelectList(repos, selectedRepoId, onSelectRepo)
                    Spacer(Modifier.height(8.dp))
                    when {
                        busy -> Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        repoFiles.isEmpty() -> Text("该仓库没有 Markdown 文件")
                        else -> LazyColumn(Modifier.heightIn(max = 280.dp)) {
                            items(repoFiles, key = { it }) { path ->
                                Text(
                                    path,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedRepoId?.let { onImport(it, path) }
                                    }.padding(vertical = 10.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

@Composable
private fun RepoSelectList(repos: List<RepoEntity>, selectedId: Long?, onSelect: (Long) -> Unit) {
    Column {
        repos.forEach { repo ->
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(repo.id) },
                color = if (repo.id == selectedId) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(repo.displayName, modifier = Modifier.padding(10.dp), maxLines = 1)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
