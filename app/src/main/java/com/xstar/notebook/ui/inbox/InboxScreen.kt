package com.xstar.notebook.ui.inbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.xstar.notebook.XstarApplication
import com.xstar.notebook.data.db.entity.InboxItemEntity
import com.xstar.notebook.ui.components.EmptyState
import com.xstar.notebook.ui.components.GradientTopBar
import com.xstar.notebook.ui.components.XStarFab
import com.xstar.notebook.viewmodel.InboxViewModel
import androidx.compose.runtime.collectAsState

@Composable
fun InboxScreen(
    onOpenDrawer: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (Long) -> Unit,
    vm: InboxViewModel = viewModel {
        val app = this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as XstarApplication
        InboxViewModel(app.container.inboxRepository)
    },
) {
    val items by vm.items.collectAsState()
    val pending = items.count { it.status == InboxItemEntity.STATUS_PENDING }
    Scaffold(
        topBar = { GradientTopBar(title = "Inbox", subtitle = "$pending 条待整理", onMenu = onOpenDrawer) },
        floatingActionButton = { XStarFab(onCreate, Icons.AutoMirrored.Rounded.Notes, "新建收集") },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(Icons.Rounded.Inbox, "Inbox 还是空的", "先收集一段文字、链接或临时想法")
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    InboxCard(item, onEdit, vm::archive, vm::delete, { vm.convertToTodo(item) {} })
                }
            }
        }
    }
}

@Composable
private fun InboxCard(
    item: InboxItemEntity,
    onEdit: (Long) -> Unit,
    onArchive: (InboxItemEntity) -> Unit,
    onDelete: (InboxItemEntity) -> Unit,
    onConvertTodo: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val icon = when (item.type) {
        InboxItemEntity.TYPE_LINK -> Icons.Rounded.Link
        InboxItemEntity.TYPE_TODO -> Icons.Rounded.TaskAlt
        else -> Icons.AutoMirrored.Rounded.Notes
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large, tonalElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().clickable { onEdit(item.id) }.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, Modifier.size(28.dp).padding(3.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.title.ifBlank { item.content.lineSequence().firstOrNull().orEmpty() }, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (item.content.isNotBlank()) Text(item.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 3, overflow = TextOverflow.Ellipsis)
                item.url?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color(0xFF3E8ED0), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Text(statusLabel(item), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "更多") }
                DropdownMenu(menu, { menu = false }) {
                    if (item.status == InboxItemEntity.STATUS_PENDING) {
                        DropdownMenuItem({ Text("转为 TODO") }, onClick = { menu = false; onConvertTodo() })
                        DropdownMenuItem({ Text("归档") }, leadingIcon = { Icon(Icons.Rounded.Archive, null) }, onClick = { menu = false; onArchive(item) })
                    }
                    DropdownMenuItem({ Text("删除", color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null) }, onClick = { menu = false; onDelete(item) })
                }
            }
        }
    }
}

private fun statusLabel(item: InboxItemEntity): String = when (item.status) {
    InboxItemEntity.STATUS_CONVERTED -> "已转换为 ${if (item.targetType == "todo") "TODO" else "Markdown"}"
    InboxItemEntity.STATUS_ARCHIVED -> "已归档"
    else -> "待整理"
}
