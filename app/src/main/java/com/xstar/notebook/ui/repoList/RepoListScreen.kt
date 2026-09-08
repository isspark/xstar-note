package com.xstar.notebook.ui.repoList

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xstar.notebook.data.db.entity.RepoEntity
import com.xstar.notebook.ui.appContainer
import com.xstar.notebook.ui.components.ConfirmDialog
import com.xstar.notebook.ui.components.EmptyState
import com.xstar.notebook.ui.components.GradientTopBar
import com.xstar.notebook.ui.components.HostAvatar
import com.xstar.notebook.ui.components.StatusChip
import com.xstar.notebook.ui.components.XStarFab
import com.xstar.notebook.viewmodel.RepoListViewModel

@Composable
fun RepoListScreen(
    onAddRepo: () -> Unit,
    onOpenRepo: (Long) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenConflicts: (Long) -> Unit = {},
    onOpenSearch: () -> Unit = {},
    vm: RepoListViewModel = viewModel {
        val app = this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY] as com.xstar.notebook.XstarApplication
        RepoListViewModel(app.container.repoRepository)
    },
) {
    val repos by vm.repos.collectAsState()
    val busyIds by vm.busyIds.collectAsState()
    val message by vm.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val container = appContainer()
    val defaultRepoId by container.settings.defaultRepoId.collectAsState()
    var pendingDelete by remember { mutableStateOf<RepoEntity?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            vm.consumeMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GradientTopBar(
                title = "笔记管理",
                subtitle = if (repos.isEmpty()) "添加一个 Git 仓库开始" else "${repos.size} 个仓库 · 点星标设为默认",
                onMenu = onOpenDrawer,
                actions = {
                    if (repos.isNotEmpty()) {
                        IconButton(onClick = onOpenSearch) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = "搜索笔记",
                                tint = Color.White,
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            XStarFab(onClick = onAddRepo, icon = Icons.Rounded.Add, contentDescription = "添加仓库")
        },
    ) { padding ->
        if (repos.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Rounded.Bookmarks,
                    title = "还没有仓库",
                    hint = "点右下角「+」添加一个 Git 仓库，\n随后点星标把它设为默认仓库",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(repos, key = { it.id }) { repo ->
                    RepoManageCard(
                        repo = repo,
                        isDefault = repo.id == defaultRepoId,
                        busy = repo.id in busyIds,
                        onClick = { onOpenRepo(repo.id) },
                        onSetDefault = {
                            container.settings.setDefaultRepo(repo.id)
                            onOpenRepo(repo.id)
                        },
                        onSync = { vm.sync(repo) },
                        onPull = { vm.pull(repo) },
                        onPush = { vm.push(repo) },
                        onDelete = { pendingDelete = repo },
                        onResolveConflicts = { onOpenConflicts(repo.id) },
                    )
                }
            }
        }
    }

    pendingDelete?.let { repo ->
        ConfirmDialog(
            title = "删除「${repo.displayName}」？",
            text = "将删除本地克隆的所有文件并清除本机凭证，不会删除远程仓库。",
            confirmText = "删除",
            onConfirm = {
                if (repo.id == defaultRepoId) container.settings.clearDefaultRepo()
                vm.delete(repo)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun RepoManageCard(
    repo: RepoEntity,
    isDefault: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    onSetDefault: () -> Unit,
    onSync: () -> Unit,
    onPull: () -> Unit,
    onPush: () -> Unit,
    onDelete: () -> Unit,
    onResolveConflicts: () -> Unit = {},
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(start = 16.dp, top = 16.dp, end = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HostAvatar(hostType = repo.hostType, size = 46.dp)
                Column(
                    Modifier.weight(1f).padding(start = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            repo.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (isDefault) {
                            Text(
                                " 默认",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        if (busy) {
                            Spacer(Modifier.size(8.dp))
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        }
                    }
                    Text(
                        repo.remoteUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (label, color) = syncBadge(repo.syncState)
                        StatusChip(label = label, color = color)
                        if (repo.syncState == "conflict") {
                            TextButton(onClick = onResolveConflicts, enabled = !busy) {
                                Text("处理冲突", color = color, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            repo.lastSyncAt?.let { "同步于 ${formatTime(it)}" } ?: "尚未同步",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                IconButton(
                    onClick = onSetDefault,
                    enabled = !busy,
                ) {
                    Icon(
                        if (isDefault) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = if (isDefault) "打开默认仓库" else "设为默认仓库",
                        tint = if (isDefault) {
                            Color(0xFFFFB300)
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "更多操作")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("同步仓库（拉取+推送）") },
                            leadingIcon = { Icon(Icons.Rounded.Sync, null) },
                            enabled = !busy,
                            onClick = {
                                menuOpen = false
                                onSync()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("拉取更新") },
                            leadingIcon = { Icon(Icons.Rounded.CloudDownload, null) },
                            enabled = !busy,
                            onClick = {
                                menuOpen = false
                                onPull()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("推送修改") },
                            leadingIcon = { Icon(Icons.Rounded.CloudUpload, null) },
                            enabled = !busy,
                            onClick = {
                                menuOpen = false
                                onPush()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("删除仓库", color = MaterialTheme.colorScheme.error) },
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
            if (busy) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                )
            }
        }
    }
}

private fun syncBadge(state: String): Pair<String, Color> = when (state) {
    "dirty" -> "有未推送修改" to Color(0xFFFF6B35)
    "syncing" -> "同步中" to Color(0xFFF2A33C)
    "conflict" -> "存在冲突" to Color(0xFFE53935)
    "error" -> "同步出错" to Color(0xFFE53935)
    else -> "已同步" to Color(0xFF00A896)
}

private fun formatTime(ts: Long): String {
    val fmt = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
    return fmt.format(java.util.Date(ts))
}
