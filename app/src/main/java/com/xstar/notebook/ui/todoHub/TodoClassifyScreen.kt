package com.xstar.notebook.ui.todoHub

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xstar.notebook.data.db.entity.CategoryEntity
import com.xstar.notebook.data.db.entity.CategorySystemEntity
import com.xstar.notebook.data.repo.TodoHubData
import com.xstar.notebook.data.repo.TodoHubRepository
import com.xstar.notebook.data.todo.TodoPresets
import com.xstar.notebook.ui.appContainer
import com.xstar.notebook.ui.components.FilledActionButton
import com.xstar.notebook.ui.components.GradientTopBar
import kotlinx.coroutines.launch

@Composable
fun TodoClassifyScreen(onBack: () -> Unit) {
    val repo = appContainer().todoHubRepository
    val scope = rememberCoroutineScope()
    val data by remember { repo.observeData() }
        .collectAsState(initial = TodoHubData())
    val snackbar = remember { SnackbarHostState() }

    var showMessage by remember { mutableStateOf<String?>(null) }
    var createSystem by remember { mutableStateOf(false) }
    var renameSystem by remember { mutableStateOf<CategorySystemEntity?>(null) }
    var addCategoryTo by remember { mutableStateOf<CategorySystemEntity?>(null) }
    var deleteSystem by remember { mutableStateOf<CategorySystemEntity?>(null) }
    var editCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    LaunchedEffect(showMessage) {
        showMessage?.let {
            snackbar.showSnackbar(it, duration = SnackbarDuration.Short)
            showMessage = null
        }
    }
    fun toast(text: String) {
        showMessage = text
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GradientTopBar(title = "TODO 分类管理", subtitle = "分类系统与自定义标签", onBack = onBack)
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "分类系统 = 一组带颜色的标签。一条 TODO 可同时打多套系统内的标签，编辑条目时点选即可。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            items(data.systems, key = { it.id }) { system ->
                val cats = data.categories.filter { it.systemId == system.id }
                SystemCard(
                    system = system,
                    categories = cats,
                    isBuiltIn = system.builtInKey != null,
                    onToggleEnabled = { enabled ->
                        scope.launch {
                            runCatching { repo.setSystemEnabled(system.id, enabled) }
                                .onFailure { toast("操作失败：" + (it.message ?: "")) }
                        }
                    },
                    onRename = { renameSystem = system },
                    onAddCategory = { addCategoryTo = system },
                    onDelete = { deleteSystem = system },
                    onEditCategory = { editCategory = it },
                )
            }
            item {
                FilledActionButton(
                    onClick = { createSystem = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                ) {
                    Icon(Icons.Rounded.Add, null)
                    Text("新建自定义分类系统", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }

    if (createSystem) {
        NewSystemDialog(
            onDismiss = { createSystem = false },
            onSave = { name, categoryNames ->
                createSystem = false
                scope.launch {
                    runCatching { repo.addCustomSystem(name, categoryNames) }
                        .onSuccess { toast("已创建分类系统「$name」") }
                        .onFailure { toast("创建失败：" + (it.message ?: "")) }
                }
            },
        )
    }
    renameSystem?.let { system ->
        TextInputDialog(
            title = "重命名分类系统",
            label = "系统名称",
            initial = system.name,
            confirmText = "保存",
            onDismiss = { renameSystem = null },
            onConfirm = { name ->
                renameSystem = null
                scope.launch {
                    runCatching { repo.renameSystem(system.id, name) }
                        .onFailure { toast("重命名失败：" + (it.message ?: "")) }
                }
            },
        )
    }
    addCategoryTo?.let { system ->
        TextInputDialog(
            title = "添加分类",
            label = "分类名称",
            initial = "",
            confirmText = "添加",
            onDismiss = { addCategoryTo = null },
            onConfirm = { name ->
                addCategoryTo = null
                scope.launch {
                    runCatching { repo.addCategory(system.id, name) }
                        .onFailure { toast("添加失败：" + (it.message ?: "")) }
                }
            },
        )
    }
    deleteSystem?.let { system ->
        AlertDialog(
            onDismissRequest = { deleteSystem = null },
            title = { Text("删除分类系统", fontWeight = FontWeight.Bold) },
            text = {
                Text("删除「${system.name}」会一并移除其下所有分类及已打上的标签，不可恢复。确定？")
            },
            confirmButton = {
                TextButton(onClick = {
                    deleteSystem = null
                    scope.launch {
                        runCatching { repo.deleteSystem(system.id) }
                            .onSuccess { toast("已删除「${system.name}」") }
                            .onFailure { toast("删除失败：" + (it.message ?: "")) }
                    }
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteSystem = null }) { Text("取消") }
            },
            shape = MaterialTheme.shapes.large,
        )
    }
    editCategory?.let { cat ->
        if (data.systems.any { it.id == cat.systemId }) {
            EditCategoryDialog(
                category = cat,
                onDismiss = { editCategory = null },
                onSave = { name, color ->
                    editCategory = null
                    scope.launch {
                        runCatching {
                            repo.renameCategory(cat.id, name)
                            repo.recolorCategory(cat.id, color)
                        }.onFailure { toast("保存失败：" + (it.message ?: "")) }
                    }
                },
                onDelete = {
                    editCategory = null
                    scope.launch {
                        runCatching { repo.deleteCategory(cat.id) }
                            .onSuccess { toast("已删除分类「${cat.name}」") }
                            .onFailure { toast("删除失败：" + (it.message ?: "")) }
                    }
                },
            )
        } else {
            editCategory = null
        }
    }
}

@Composable
private fun SystemCard(
    system: CategorySystemEntity,
    categories: List<CategoryEntity>,
    isBuiltIn: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onRename: () -> Unit,
    onAddCategory: () -> Unit,
    onDelete: () -> Unit,
    onEditCategory: (CategoryEntity) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isBuiltIn) Icons.Rounded.SettingsSuggest else Icons.Rounded.Label,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            system.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (isBuiltIn) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = CircleShape,
                                modifier = Modifier.padding(start = 8.dp),
                            ) {
                                Text(
                                    "内置",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    Text(
                        if (system.enabled) "已启用 · ${categories.size} 个分类" else "已停用（数据保留，仅不出现于标签选择）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                if (!isBuiltIn) {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "系统操作")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("重命名系统") },
                                leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                                onClick = {
                                    menuOpen = false
                                    onRename()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("添加分类") },
                                leadingIcon = { Icon(Icons.Rounded.Add, null) },
                                onClick = {
                                    menuOpen = false
                                    onAddCategory()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("删除系统", color = MaterialTheme.colorScheme.error) },
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
                } else {
                    IconButton(onClick = onAddCategory) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "添加分类",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Switch(checked = system.enabled, onCheckedChange = onToggleEnabled)
            }
            if (categories.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                categories.forEach { cat ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onEditCategory(cat) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(12.dp).background(classColor(cat), CircleShape))
                        Text(
                            cat.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f).padding(start = 10.dp),
                        )
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = "编辑（改颜色/改名/删除）",
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NewSystemDialog(
    onDismiss: () -> Unit,
    onSave: (String, List<String>) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var lines by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建自定义分类系统", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("系统名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
                OutlinedTextField(
                    value = lines,
                    onValueChange = { lines = it },
                    label = { Text("分类（每行一个，颜色自动轮换）") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    minLines = 3,
                )
                Text(
                    "示例：\n深度工作\n常规处理\n琐碎待办",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), lines.lines().map { it.trim() }) },
                enabled = name.isNotBlank() && lines.isNotBlank(),
            ) { Text("创建", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        shape = MaterialTheme.shapes.large,
    )
}

@Composable
private fun TextInputDialog(
    title: String,
    label: String,
    initial: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.trim()) }, enabled = value.isNotBlank()) {
                Text(confirmText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        shape = MaterialTheme.shapes.large,
    )
}

@Composable
private fun EditCategoryDialog(
    category: CategoryEntity,
    onDismiss: () -> Unit,
    onSave: (String, Long) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember { mutableStateOf(category.name) }
    var color by remember { mutableStateOf(category.colorArgb) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑分类", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
                Text(
                    "颜色",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.outline,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TodoPresets.PALETTE.forEach { c ->
                        val selected = c == color
                        Box(
                            Modifier
                                .size(30.dp)
                                .background(Color(c).copy(alpha = if (selected) 1f else 0.55f), CircleShape)
                                .clickable { color = c }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Box(Modifier.size(10.dp).background(Color.White, CircleShape))
                            }
                        }
                    }
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
                    Text("删除该分类", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 6.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), color) }, enabled = name.isNotBlank()) {
                Text("保存", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        shape = MaterialTheme.shapes.large,
    )
}

private fun classColor(category: CategoryEntity): Color = Color(category.colorArgb)
