package com.xstar.notebook.ui.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xstar.notebook.XstarApplication
import com.xstar.notebook.data.db.entity.InboxItemEntity
import com.xstar.notebook.ui.components.FilledActionButton
import com.xstar.notebook.ui.components.GradientTopBar
import com.xstar.notebook.viewmodel.InboxViewModel

@Composable
fun InboxEditorScreen(
    itemId: Long,
    onBack: () -> Unit,
    vm: InboxViewModel = viewModel {
        val app = this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as XstarApplication
        InboxViewModel(app.container.inboxRepository)
    },
) {
    val existing = vm.items.value.firstOrNull { it.id == itemId }
    var type by remember(existing?.id) { mutableStateOf(existing?.type ?: InboxItemEntity.TYPE_TEXT) }
    var title by remember(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var content by remember(existing?.id) { mutableStateOf(existing?.content.orEmpty()) }
    var url by remember(existing?.id) { mutableStateOf(existing?.url.orEmpty()) }
    val editing = existing != null

    Scaffold(
        topBar = {
            GradientTopBar(
                title = if (editing) "编辑收集" else "新建收集",
                subtitle = "先记录，稍后整理",
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("内容类型", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(InboxItemEntity.TYPE_TEXT to "文本", InboxItemEntity.TYPE_LINK to "链接", InboxItemEntity.TYPE_TODO to "TODO").forEach { (value, label) ->
                    FilterChip(selected = type == value, onClick = { type = value }, label = { Text(label) })
                }
            }
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("标题（可选）") }, singleLine = true)
            if (type == InboxItemEntity.TYPE_LINK) {
                OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("链接") }, leadingIcon = { Icon(Icons.Rounded.Link, null) }, singleLine = true)
            }
            OutlinedTextField(content, { content = it }, Modifier.fillMaxWidth(), label = { Text(if (type == InboxItemEntity.TYPE_TODO) "备注（可选）" else "内容") }, minLines = 8)
            FilledActionButton(
                onClick = {
                    val normalizedUrl = url.takeIf { type == InboxItemEntity.TYPE_LINK }
                    if (editing) vm.update(existing!!, title, content, normalizedUrl, onBack)
                    else vm.add(type, title, content, normalizedUrl, onBack)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("保存到 Inbox") }
        }
    }
}
