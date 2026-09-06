package com.xstar.notebook.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xstar.notebook.ui.appContainer
import com.xstar.notebook.ui.components.ConfirmDialog
import com.xstar.notebook.ui.components.EmptyState
import com.xstar.notebook.ui.components.FileTypeIcon
import com.xstar.notebook.ui.components.GradientTopBar
import com.xstar.notebook.ui.components.XStarFab
import com.xstar.notebook.ui.components.FileKind
import com.xstar.notebook.viewmodel.BrowseViewModel
import java.io.File
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    repoId: Long,
    onOpenDrawer: () -> Unit,
    onOpenNode: (FileKind, String) -> Unit,
    vm: BrowseViewModel = viewModel {
        val app = this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY] as com.xstar.notebook.XstarApplication
        BrowseViewModel(repoId, app.container.repoRepository)
    },
) {
    val state by vm.state.collectAsState()
    var previewFile by remember { mutableStateOf<File?>(null) }
    var renameTarget by remember { mutableStateOf<File?>(null) }
    var deleteTarget by remember { mutableStateOf<File?>(null) }
    var showNewFile by remember { mutableStateOf(false) }
    var busyPath by remember { mutableStateOf<String?>(null) }
    val container = appContainer()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val repo by vm.repo.collectAsState()
    val dirty = repo?.syncState == "dirty"

    // 进入编辑器等子页面后再返回时，自动刷新当前目录（避免“新建后不显示”）
    DisposableEffect(Unit) {
        onDispose { vm.markNeedsRefresh() }
    }
    LaunchedEffect(Unit) { vm.refreshIfNeeded() }

    fun createNode(folder: String, name: String, isDir: Boolean) {
        if (busyPath != null) return
        busyPath = "new"
        val cleanFolder = folder.trim().replace("\\", "/")
        val cleanName = name.trim()
        scope.launch {
            try {
                val repo = container.repoRepository.getRepo(repoId)
                if (repo != null) {
                    val rel = container.repoRepository.createFileOrFolder(repo, cleanFolder, cleanName, isDir)
                    val synced = container.repoRepository.getRepo(repoId)?.syncState == "idle"
                    val base = if (isDir) "已创建文件夹" else "已创建"
                    snackbar.showSnackbar(
                        if (synced) "$base $cleanName 并同步到 Git"
                        else "$base $cleanName（本地已提交，推送未完成，可稍后重试同步）",
                        duration = SnackbarDuration.Short,
                    )
                    vm.load(state.currentDir)
                    if (!isDir) {
                        val kind = com.xstar.notebook.ui.components.FileTypes.kind(rel.substringAfterLast('/'))
                        if (kind == FileKind.MD || kind == FileKind.TEXT || kind == FileKind.CODE) {
                            onOpenNode(kind, rel)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(
                    "BrowseScreen",
                    "createNode fail repoId=$repoId folder='$cleanFolder' name='$cleanName' isDir=$isDir currentDir='${state.currentDir}'",
                    e,
                )
                snackbar.showSnackbar(e.message?.take(160) ?: "创建失败", duration = SnackbarDuration.Short)
            } finally {
                busyPath = null
            }
        }
    }

    fun runFileOp(file: File, block: suspend (com.xstar.notebook.data.db.entity.RepoEntity, String) -> Boolean) {
        if (busyPath != null) return
        val rel = relOf(state.currentDir, file.name)
        busyPath = rel
        scope.launch {
            try {
                val repo = container.repoRepository.getRepo(repoId)
                if (repo != null) {
                    val pushed = block(repo, rel)
                    snackbar.showSnackbar(
                        if (pushed) "已修改并同步到 Git" else "已修改并提交（未能推送，可在仓库管理同步）",
                        duration = SnackbarDuration.Short,
                    )
                }
            } catch (e: Exception) {
                snackbar.showSnackbar(e.message?.take(160) ?: "操作失败", duration = SnackbarDuration.Short)
            } finally {
                busyPath = null
                vm.load(state.currentDir)
            }
        }
    }

    val sortedFiles = remember(state.files) {
        state.files.sortedWith(
            compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GradientTopBar(
                title = state.currentDir.substringAfterLast('/').ifEmpty { "根目录" },
                subtitle = state.pathSegments.size.let { if (it == 0) "全部文件" else "${it + 1} 层 · ${state.files.size} 项" },
                onMenu = onOpenDrawer,
                actions = {
                    val syncingNow by vm.syncing.collectAsState()
                    Box {
                        if (syncingNow) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(horizontal = 10.dp).size(22.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                        } else {
                            IconButton(onClick = {
                                scope.launch {
                                    vm.syncRefresh()?.let {
                                        snackbar.showSnackbar(it, duration = SnackbarDuration.Short)
                                    }
                                }
                            }) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "同步刷新", tint = Color.White)
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            XStarFab(
                onClick = { showNewFile = true },
                icon = Icons.Rounded.Add,
                contentDescription = "新建文件",
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = {
                scope.launch {
                    vm.syncRefresh()?.let {
                        snackbar.showSnackbar(it, duration = SnackbarDuration.Short)
                    }
                }
            },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Column(Modifier.fillMaxSize()) {
                if (busyPath != null) {
                    SyncProgressHint()
                }
                if (dirty) {
                    DirtyPushBanner(
                        onRetry = {
                            vm.retryPush()
                            scope.launch {
                                snackbar.showSnackbar("已尝试推送，请留意状态变化", duration = SnackbarDuration.Short)
                            }
                        },
                    )
                }
                Breadcrumbs(state) { vm.navigate(it) }

                when {
                    state.loading && state.files.isEmpty() -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                    state.files.isEmpty() -> Box(
                        Modifier.fillMaxSize().padding(bottom = 120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(
                            icon = Icons.Rounded.FolderOpen,
                            title = "这里空空的",
                            hint = "下拉刷新试试，或去别处看看",
                        )
                    }
                    else -> LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            vertical = 8.dp,
                        ),
                    ) {
                        items(sortedFiles, key = { it.absolutePath }) { file ->
                            FileRow(
                                file = file,
                                busy = busyPath != null,
                                onClick = {
                                    if (file.isDirectory) {
                                        vm.navigate(
                                            if (state.currentDir.isBlank()) file.name
                                            else "${state.currentDir}/${file.name}",
                                        )
                                    } else {
                                        val kind = com.xstar.notebook.ui.components.FileTypes.kind(file.name)
                                        if (kind == FileKind.IMAGE) previewFile = file
                                        else onOpenNode(kind, relOf(state.currentDir, file.name))
                                    }
                                },
                                onRename = {
                                    renameTarget = file
                                },
                                onDelete = { deleteTarget = file },
                            )
                        }
                    }
                }
            }
        }
    }

    previewFile?.let { file ->
        ImagePreviewDialog(file = file, onDismiss = { previewFile = null })
    }

    renameTarget?.let { target ->
        RenameDialog(
            currentName = target.name,
            isDirectory = target.isDirectory,
            busy = busyPath != null,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                renameTarget = null
                runFileOp(target) { repo, rel ->
                    container.repoRepository.renameNode(repo, rel, newName)
                }
            },
        )
    }

    if (showNewFile) {
        NewFileDialog(
            defaultFolder = state.currentDir,
            subDirectories = state.files.filter { it.isDirectory }.map { it.name }.sorted(),
            busy = busyPath != null,
            onDismiss = { showNewFile = false },
            onCreate = { folder, name, isDir ->
                showNewFile = false
                createNode(folder, name, isDir)
            },
        )
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "删除「${target.name}」？",
            text = if (target.isDirectory) {
                "将删除该目录及其中全部文件，并提交到 Git（不删除远程历史）。"
            } else {
                "将从仓库中删除该文件并提交到 Git。"
            },
            confirmText = "删除",
            onConfirm = {
                deleteTarget = null
                runFileOp(target) { repo, rel ->
                    container.repoRepository.deleteNode(repo, rel)
                }
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun Breadcrumbs(state: com.xstar.notebook.viewmodel.BrowseUiState, onNavigate: (String) -> Unit) {    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BreadcrumbChip("根", selected = state.currentDir.isBlank()) { onNavigate("") }
        state.pathSegments.forEachIndexed { index, seg ->
            Text("/", color = MaterialTheme.colorScheme.outlineVariant, fontWeight = FontWeight.Bold)
            BreadcrumbChip(
                seg,
                selected = index == state.pathSegments.lastIndex,
            ) {
                onNavigate(state.pathSegments.take(index + 1).joinToString("/"))
            }
        }
    }
}

@Composable
private fun BreadcrumbChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun FileRow(
    file: File,
    busy: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = !busy, onClick = onClick)
            .padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FileTypeIcon(isDirectory = file.isDirectory, name = file.name)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                file.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (file.isDirectory) "文件夹" else "${readableSize(file.length())} · ${file.extension.ifEmpty { "文件" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }, enabled = !busy) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "更多操作")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("重命名") },
                    leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                    onClick = {
                        menuOpen = false
                        onRename()
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

@Composable
private fun RenameDialog(
    currentName: String,
    isDirectory: Boolean,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isDirectory) "重命名目录" else "重命名文件", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("新名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank() && !name.contains('/') && !name.contains('\\') && !busy,
            ) {
                Text("确定", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        shape = MaterialTheme.shapes.large,
    )
}

@Composable
private fun NewFileDialog(
    defaultFolder: String,
    subDirectories: List<String>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String, Boolean) -> Unit,
) {
    var isDir by remember { mutableStateOf(false) }
    var folder by remember { mutableStateOf(defaultFolder) }
    var name by remember { mutableStateOf("") }

    fun childRel(dir: String): String = if (defaultFolder.isBlank()) dir else "$defaultFolder/$dir"

    val choices = remember(defaultFolder, subDirectories) {
        buildList {
            if (defaultFolder.isBlank()) {
                add("根目录" to "")
            } else {
                add("根目录" to "")
                add("当前目录：$defaultFolder" to defaultFolder)
            }
            subDirectories.forEach { dir ->
                add("$dir/" to childRel(dir))
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isDir) "新建文件夹" else "新建文件", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TypeChoice(label = "文件", selected = !isDir) { isDir = false }
                    TypeChoice(label = "文件夹", selected = isDir) { isDir = true }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isDir) "文件夹名称" else "文件名") },
                    placeholder = { Text(if (isDir) "新建文件夹" else "new") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
                if (!isDir && name.isNotBlank() && !name.contains('.')) {
                    Text(
                        "未填写扩展名，将创建为 Markdown 文件（${name.trim()}.md）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    "创建位置（点击选择）",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.outline,
                )
                choices.forEach { (label, rel) ->
                    val selected = rel == folder
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { folder = rel },
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
                            Icon(
                                Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(start = 8.dp),
                            )
                            if (selected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(folder, name.trim(), isDir) },
                enabled = name.isNotBlank() &&
                    !name.contains('/') && !name.contains('\\') && !busy,
            ) {
                Text("创建", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        shape = MaterialTheme.shapes.large,
    )
}

@Composable
private fun TypeChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun ImagePreviewDialog(file: File, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(
                model = file,
                contentDescription = file.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.small),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = Color.White)
            }
            Text(
                file.name,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        ),
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun DirtyPushBanner(onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 2.dp, end = 8.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "本地有修改尚未推送到远程",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) {
                Text("立即推送", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SyncProgressHint() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
                Text(
                    "  正在写入并同步到 Git，请稍候…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent,
            )
        }
    }
}

private fun readableSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format(Locale.ROOT, "%.1f MB", bytes / 1024f / 1024f)
    bytes >= 1024 -> String.format(Locale.ROOT, "%.1f KB", bytes / 1024f)
    else -> "$bytes B"
}

private fun relOf(dir: String, name: String): String = if (dir.isBlank()) name else "$dir/$name"
