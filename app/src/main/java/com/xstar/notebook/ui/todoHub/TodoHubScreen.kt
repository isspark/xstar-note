package com.xstar.notebook.ui.todoHub

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xstar.notebook.data.db.entity.LocalTodoEntity
import com.xstar.notebook.data.db.entity.RepoEntity
import com.xstar.notebook.data.repo.TodoHubRepository
import com.xstar.notebook.ui.appContainer
import com.xstar.notebook.ui.components.EmptyState
import com.xstar.notebook.ui.components.GradientTopBar
import com.xstar.notebook.ui.components.OutlinedActionButton
import com.xstar.notebook.ui.components.XStarFab
import com.xstar.notebook.viewmodel.TodoHubViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TodoHubScreen(
    onOpenDrawer: () -> Unit,
    vm: TodoHubViewModel = viewModel {
        val app = this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY] as com.xstar.notebook.XstarApplication
        TodoHubViewModel(app.container.repoRepository, app.container.todoHubRepository)
    },
) {
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var editorItem by remember { mutableStateOf<LocalTodoEntity?>(null) }
    var showExport by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it, duration = SnackbarDuration.Short)
            vm.consumeMessage()
        }
    }
    LaunchedEffect(showImport, state.selectedRepoId) {
        if (showImport) state.selectedRepoId?.let(vm::refreshFiles)
    }

    val done = state.todos.count { it.done }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GradientTopBar(
                title = "我的 TODO",
                subtitle = if (state.todos.isEmpty()) "独立于 Git 的本地清单" else "${state.todos.size - done} 项待办 · $done 已完成",
                onMenu = onOpenDrawer,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            XStarFab(
                onClick = { editorItem = LocalTodoEntity(title = "") },
                icon = Icons.Rounded.Add,
                contentDescription = "新建 TODO",
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedActionButton(
                    onClick = { showExport = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.CloudUpload, null)
                    Text("导出到仓库", modifier = Modifier.padding(start = 6.dp))
                }
                OutlinedActionButton(
                    onClick = { showImport = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.CloudDownload, null)
                    Text("从仓库导入", modifier = Modifier.padding(start = 6.dp))
                }
            }

            if (state.todos.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(bottom = 140.dp), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Rounded.Checklist,
                        title = "清单还是空的",
                        hint = "点右下角「+」，随手记一条 TODO",
                    )
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.todos, key = { it.id }) { item ->
                        TodoItemCard(
                            item = item,
                            onToggle = { vm.toggle(item) },
                            onEdit = { editorItem = item },
                            onDelete = { vm.delete(item) },
                        )
                    }
                }
            }
        }
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
    editorItem?.let { item ->
        TodoEditorDialog(
            title = item.title,
            note = item.note,
            isNew = item.id == 0L,
            onDismiss = { editorItem = null },
            onSave = { title, note ->
                editorItem = null
                if (item.id == 0L) vm.add(title, note) else vm.update(item, title, note)
            },
        )
    }
}

@Composable
private fun TodoItemCard(
    item: LocalTodoEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (item.done) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = item.done, onCheckedChange = { onToggle() })
            Column(
                Modifier.weight(1f).clickable(onClick = onEdit).padding(vertical = 8.dp),
            ) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (item.done) FontWeight.Normal else FontWeight.Medium,
                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                    color = if (item.done) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.note.isNotBlank()) {
                    Text(
                        item.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    formatDate(item.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "更多")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TodoEditorDialog(
    title: String,
    note: String,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var titleText by remember { mutableStateOf(title) }
    var noteText by remember { mutableStateOf(note) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "新建 TODO" else "编辑 TODO", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(titleText.trim(), noteText.trim()) },
                enabled = titleText.isNotBlank(),
            ) {
                Text("保存", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        shape = MaterialTheme.shapes.large,
    )
}

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
                Text(
                    "选择目标仓库：",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                if (repos.isEmpty()) {
                    Text(
                        "还没有仓库，请先在「仓库管理」中添加。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                } else {
                    RepoSelectChips(repos, repoId) { repoId = it }
                    OutlinedTextField(
                        value = folder,
                        onValueChange = { folder = it },
                        label = { Text("导出目录（留空为根目录）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        shape = MaterialTheme.shapes.medium,
                    )
                    Text(
                        "文件名：${TodoHubRepository.defaultFileName()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { repoId?.let { onExport(it, folder) } },
                enabled = repoId != null && !busy,
            ) {
                Text("导出并提交", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        shape = MaterialTheme.shapes.large,
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
                    Text(
                        "还没有仓库，请先在「仓库管理」中添加。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                } else {
                    Text(
                        "选择仓库：",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    RepoSelectChips(repos, selectedRepoId, onSelectRepo)
                    Text(
                        "选择要导入的 Markdown 文件（解析 - [ ] / - [x] 任务）：",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                    )
                    if (busy) {
                        Box(
                            Modifier.fillMaxWidth().heightIn(min = 120.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (repoFiles.isEmpty()) {
                        Box(
                            Modifier.fillMaxWidth().heightIn(min = 120.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "该仓库没有 .md 文件",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(repoFiles, key = { it }) { path ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedRepoId?.let { onImport(it, path) }
                                    },
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.small,
                                ) {
                                    Text(
                                        path,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
        shape = MaterialTheme.shapes.large,
    )
}

@Composable
private fun RepoSelectChips(
    repos: List<RepoEntity>,
    selectedRepoId: Long?,
    onSelect: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.heightIn(max = 160.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(repos, key = { it.id }) { repo ->
            val selected = repo.id == selectedRepoId
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(repo.id) },
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = MaterialTheme.shapes.small,
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        repo.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun formatDate(ts: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
