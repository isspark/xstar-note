package com.xstar.notebook.ui.mdView

import android.content.Intent
import android.net.Uri
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.FormatStrikethrough
import androidx.compose.material.icons.rounded.HorizontalRule
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xstar.notebook.ui.appContainer
import com.xstar.notebook.ui.components.FilledActionButton
import com.xstar.notebook.ui.components.GradientTopBar
import com.xstar.notebook.ui.components.XStarFab
import com.xstar.notebook.viewmodel.MdViewViewModel
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.LinkResolver
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.image.coil.CoilImagesPlugin
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TocEntry(val level: Int, val title: String, val lineIndex: Int)

private val EncodingChoices = listOf(
    "UTF-8", "GBK", "GB18030", "Big5",
    "UTF-16LE", "UTF-16BE", "ISO-8859-1", "US-ASCII",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MdViewScreen(
    repoId: Long,
    relPath: String,
    onBack: () -> Unit,
    onOpenTodo: (Long, String) -> Unit,
    onOpenDoc: (Long, String) -> Unit = { _, _ -> },
    onOpenMermaid: (Long, String, Int) -> Unit = { _, _, _ -> },
    markdownMode: Boolean = true,
    vm: MdViewViewModel = viewModel {
        val app = this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY] as com.xstar.notebook.XstarApplication
        MdViewViewModel(repoId, relPath, app.container.repoRepository)
    },
) {
    val state by vm.state.collectAsState()
    val message by vm.message.collectAsState()
    val context = LocalContext.current
    val container = (context.applicationContext as com.xstar.notebook.XstarApplication).container
    val scope = rememberCoroutineScope()
    val snackbar = androidx.compose.material3.SnackbarHostState()

    var editingState by remember { mutableStateOf<TextFieldValue?>(null) }
    val editing: Boolean = editingState != null
    var showToc by remember { mutableStateOf(false) }
    var showCharsetMenu by remember { mutableStateOf(false) }
    var showBacklinks by remember { mutableStateOf(false) }
    var showImageSource by remember { mutableStateOf(false) }
    var draftOffer by remember { mutableStateOf(false) }

    val baseDir: String = remember(relPath) {
        val slash = relPath.lastIndexOf('/')
        if (slash >= 0) relPath.substring(0, slash + 1) else ""
    }
    val repoDir: File = remember { container.gitClient.absolutePath(repoId, "") }
    val mdIndex = remember(repoDir) { buildMdIndex(repoDir) }

    val prepared: PreparedDoc = remember(state.content) {
        if (markdownMode) {
            MdDocumentPreparer.prepare(state.content, baseDir, repoDir, mdIndex)
        } else {
            PreparedDoc(markdown = state.content, mermaidBlocks = emptyList())
        }
    }

    val markwon: Markwon = remember(context, markdownMode) {
        Markwon.builder(context)
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(CoilImagesPlugin.create(context))
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                    builder.linkResolver(LinkResolver { view, link -> handleInternalLink(link, context, repoDir, repoId, relPath, onOpenDoc, onOpenMermaid) })
                }
            })
            .build()
    }

    val scrollRef = remember { mutableStateOf<ScrollView?>(null) }
    val textRef = remember { mutableStateOf<TextView?>(null) }
    val editorFocus = remember { FocusRequester() }

    // 顶部提示消息
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it, duration = androidx.compose.material3.SnackbarDuration.Short)
            vm.consumeMessage()
        }
    }

    LaunchedEffect(editing) {
        if (editing) {
            delay(150)
            runCatching { editorFocus.requestFocus() }
        }
    }

    // 提示恢复未保存草稿
    LaunchedEffect(state.loading, state.error, editing) {
        if (!state.loading && state.error == null && !editing && markdownMode && !draftOffer) {
            draftOffer = DraftStore.exists(context, repoId, relPath)
        }
    }

    // 编辑时自动保存草稿
    val editorText = editingState?.text
    LaunchedEffect(editorText, editing) {
        if (editing && editorText != null && state.content != editorText) {
            delay(700)
            DraftStore.save(context, repoId, relPath, editorText)
        }
    }

    fun startEditing(initial: String? = null) {
        val base = initial ?: state.content
        editingState = TextFieldValue(base, TextRange(base.length, base.length))
        draftOffer = false
    }

    fun saveAndClose() {
        val text = editingState?.text ?: return
        if (text.isBlank() && state.content.isNotBlank()) return
        vm.save(text)
        DraftStore.clear(context, repoId, relPath)
    }

    fun abandonEdit() {
        scope.launch {
            withContext(Dispatchers.IO) { vm.discardAssets() }
            DraftStore.clear(context, repoId, relPath)
            editingState = null
        }
    }

    fun insertImageMarkdown(rel: String) {
        val current = editingState ?: return
        val name = rel.substringAfterLast('/')
        val insert = "![$name]($rel)"
        val at = current.selection.min
        val newText = current.text.substring(0, at) + insert + current.text.substring(at)
        val newSel = at + insert.length
        editingState = current.copy(text = newText, selection = TextRange(newSel, newSel))
    }

    fun loadImageBytes(uri: Uri): ByteArray? {
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        return bytes
    }

    fun persistImage(bytes: ByteArray, ext: String) {
        scope.launch {
            val rel = vm.importAssetImage(baseDir, bytes, ext)
            if (rel == null) {
                snackbar.showSnackbar("图片写入失败", duration = androidx.compose.material3.SnackbarDuration.Short)
            } else {
                insertImageMarkdown(rel)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        showImageSource = false
        if (uri != null) {
            val bytes = loadImageBytes(uri)
            val ext = (context.contentResolver.getType(uri) ?: "image/png")
                .substringAfterLast('/').takeIf { it.length in 1..6 } ?: "png"
            if (bytes != null) persistImage(bytes, ext)
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview(),
    ) { bitmap ->
        showImageSource = false
        if (bitmap != null) {
            val out = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, out)
            persistImage(out.toByteArray(), "jpg")
        }
    }

    fun pickImageFromGallery() {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun pickImageFromCamera() {
        cameraLauncher.launch(null)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GradientTopBar(
                title = relPath.substringAfterLast('/'),
                subtitle = when {
                    editing -> "编辑中 · 保存后写入并同步 Git"
                    markdownMode -> relPath.substringBeforeLast('/', "").ifEmpty { "根目录" }
                    else -> "代码/文本 · 编码 ${state.charsetName ?: "自动"}"
                },
                onBack = onBack,
                actions = {
                    if (editing) {
                        IconButton(onClick = ::abandonEdit) {
                            Icon(Icons.Rounded.Close, contentDescription = "放弃编辑", tint = Color.White)
                        }
                        IconButton(onClick = ::saveAndClose) {
                            if (state.saving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(4.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                            } else {
                                Icon(Icons.Rounded.Check, contentDescription = "保存并同步", tint = Color.White)
                            }
                        }
                    } else {
                        // 编码选择对所有文档可见（markdown 与代码/文本都支持）
                        Box {
                            Surface(
                                modifier = Modifier.clickable { showCharsetMenu = true },
                                color = Color.White.copy(alpha = 0.18f),
                                shape = androidx.compose.foundation.shape.CircleShape,
                            ) {
                                Text(
                                    state.charsetName ?: "编码：自动",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                            DropdownMenu(
                                expanded = showCharsetMenu,
                                onDismissRequest = { showCharsetMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("自动检测") },
                                    onClick = {
                                        showCharsetMenu = false
                                        vm.loadWithCharset(null)
                                    },
                                )
                                EncodingChoices.forEach { enc ->
                                    DropdownMenuItem(
                                        text = { Text(enc) },
                                        onClick = {
                                            showCharsetMenu = false
                                            vm.loadWithCharset(enc)
                                        },
                                    )
                                }
                            }
                        }
                        if (markdownMode) {
                            IconButton(onClick = {
                                vm.loadBacklinks()
                                showBacklinks = true
                            }) {
                                Icon(Icons.Rounded.Link, contentDescription = "反向链接", tint = Color.White)
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbar) },
        floatingActionButton = {
            if (!state.loading && state.error == null && !editing) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (markdownMode && parseToc(state.content).isNotEmpty()) {
                        XStarFab(
                            onClick = { showToc = true },
                            icon = Icons.Rounded.MenuBook,
                            contentDescription = "文档导航",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    if (markdownMode) {
                        XStarFab(
                            onClick = { onOpenTodo(repoId, relPath) },
                            icon = Icons.Rounded.Checklist,
                            contentDescription = "任务清单",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    XStarFab(
                        onClick = { startEditing() },
                        icon = Icons.Rounded.Edit,
                        contentDescription = "编辑",
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (editing) {
                EditorToolbar(
                    onTool = { tool -> editingState = editingState?.let { applyMarkdownTool(it, tool) } },
                    onInsertImage = { showImageSource = true },
                )
            }
            Box(Modifier.fillMaxSize()) {
                when {
                    state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    state.error != null -> Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("加载失败：${state.error}", color = MaterialTheme.colorScheme.error)
                        FilledActionButton(onClick = vm::load) {
                            Text("重试", fontWeight = FontWeight.Bold)
                        }
                    }
                    editing -> MarkdownEditor(
                        value = editingState ?: TextFieldValue(""),
                        onValueChange = { editingState = it },
                        focusRequester = editorFocus,
                        modifier = Modifier.fillMaxSize(),
                    )
                    markdownMode -> MarkdownBody(
                        markwon = markwon,
                        markdown = prepared.markdown.ifEmpty { "*（空文档）*" },
                        scrollRef = scrollRef,
                        textRef = textRef,
                        modifier = Modifier.fillMaxSize(),
                    )
                    else -> PlainCodeBody(
                        content = state.content,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (draftOffer && markdownMode && !state.loading) {
                    DraftRestoreBanner(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                        onRestore = {
                            val content = DraftStore.load(context, repoId, relPath)
                            startEditing(content ?: state.content)
                            draftOffer = false
                        },
                        onDiscard = {
                            DraftStore.clear(context, repoId, relPath)
                            draftOffer = false
                        },
                    )
                }
            }
        }
    }

    if (showImageSource) {
        AlertDialog(
            onDismissRequest = { showImageSource = false },
            title = { Text("插入图片", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = ::pickImageFromGallery) {
                        Icon(Icons.Rounded.Image, contentDescription = null)
                        Text("  从相册选择")
                    }
                    TextButton(onClick = ::pickImageFromCamera) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                        Text("  拍照")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImageSource = false }) { Text("取消") }
            },
        )
    }

    if (showToc && markdownMode) {
        val entries = parseToc(state.content)
        ModalBottomSheet(
            onDismissRequest = { showToc = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Text(
                "文档导航",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (entries.isEmpty()) {
                Text(
                    "本文档没有标题结构",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(20.dp),
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(entries) { entry ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                showToc = false
                                jumpToHeading(textRef.value, scrollRef.value, entries, entry.title)
                            },
                            color = Color.Transparent,
                        ) {
                            Text(
                                entry.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (entry.level <= 2) FontWeight.Bold else FontWeight.Medium,
                                color = if (entry.level <= 2) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp + ((entry.level - 1) * 10).dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBacklinks && markdownMode) {
        val total = vm.state.value.backlinks.size + 1
        ModalBottomSheet(
            onDismissRequest = { showBacklinks = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        ) {
            Text(
                "引用本文档的笔记",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            when {
                state.backlinksLoading -> Box(
                    Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                state.backlinks.isEmpty() -> Text(
                    "暂无其它笔记引用本文档",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(20.dp),
                )
                else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(state.backlinks, key = { it }) { rel ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                showBacklinks = false
                                onOpenDoc(repoId, rel)
                            },
                            color = Color.Transparent,
                        ) {
                            Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                                Text(
                                    rel.substringAfterLast('/'),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    rel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// 工具栏
// ------------------------------------------------------------------

@Composable
private fun EditorToolbar(
    onTool: (MdTool) -> Unit,
    onInsertImage: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item { HeadingButton("H1") { onTool(MdTool.H1) } }
            item { HeadingButton("H2") { onTool(MdTool.H2) } }
            item { HeadingButton("H3") { onTool(MdTool.H3) } }
            item {
                ToolIconButton(Icons.Rounded.FormatBold, "加粗") { onTool(MdTool.BOLD) }
            }
            item {
                ToolIconButton(Icons.Rounded.FormatItalic, "斜体") { onTool(MdTool.ITALIC) }
            }
            item {
                ToolIconButton(Icons.Rounded.FormatStrikethrough, "删除线") { onTool(MdTool.STRIKE) }
            }
            item {
                ToolIconButton(Icons.Rounded.FormatQuote, "引用") { onTool(MdTool.QUOTE) }
            }
            item {
                ToolIconButton(Icons.Rounded.FormatListBulleted, "无序列表") { onTool(MdTool.BULLET) }
            }
            item {
                ToolIconButton(Icons.Rounded.FormatListNumbered, "有序列表") { onTool(MdTool.NUMBER) }
            }
            item {
                ToolIconButton(Icons.Rounded.Checklist, "任务列表") { onTool(MdTool.TASK) }
            }
            item {
                ToolIconButton(Icons.Rounded.Code, "代码块") { onTool(MdTool.CODE_BLOCK) }
            }
            item {
                ToolIconButton(Icons.Rounded.Link, "插入链接") { onTool(MdTool.LINK) }
            }
            item {
                ToolIconButton(Icons.Rounded.HorizontalRule, "分隔线") { onTool(MdTool.HR) }
            }
            item {
                ToolIconButton(Icons.Rounded.Image, "插入图片") { onInsertImage() }
            }
        }
    }
}

@Composable
private fun HeadingButton(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick).padding(2.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ToolIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = desc)
    }
}

@Composable
private fun DraftRestoreBanner(
    modifier: Modifier = Modifier,
    onRestore: () -> Unit,
    onDiscard: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.large,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "有未保存的草稿",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRestore) { Text("恢复", fontWeight = FontWeight.Bold) }
            TextButton(onClick = onDiscard) { Text("丢弃") }
        }
    }
}

// ------------------------------------------------------------------
// 编辑器与渲染
// ------------------------------------------------------------------

@Composable
private fun MarkdownEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    focusRequester: androidx.compose.ui.focus.FocusRequester,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.imePadding(), color = MaterialTheme.colorScheme.surface) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
            ),
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .padding(16.dp),
        )
    }
}

/** 点击 TextView 上的链接（兼容可选择文本模式，不需要替换 MovementMethod）。 */
private class LinkTapWatcher : View.OnTouchListener {
    private var downX = 0f
    private var downY = 0f
    private var tracking = false
    private var downTime = 0L

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                downTime = System.currentTimeMillis()
                tracking = true
            }
            MotionEvent.ACTION_UP -> {
                if (!tracking) return false
                tracking = false
                val tv = v as? TextView ?: return false
                val slop = ViewConfiguration.get(v.context).scaledTouchSlop.toFloat()
                if (kotlin.math.abs(event.x - downX) > slop || kotlin.math.abs(event.y - downY) > slop) return false
                if (System.currentTimeMillis() - downTime > 400L) return false
                val off = tv.getOffsetForPosition(event.x, event.y)
                val text = tv.text
                if (off >= 0 && text is Spanned) {
                    val spans = text.getSpans(off, off, URLSpan::class.java)
                    if (spans.isNotEmpty()) {
                        (spans.first() as? ClickableSpan)?.onClick(tv) ?: spans.first().onClick(tv)
                        return true
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> tracking = false
        }
        return false
    }
}

@Composable
private fun MarkdownBody(
    markwon: Markwon,
    markdown: String,
    scrollRef: androidx.compose.runtime.MutableState<ScrollView?>,
    textRef: androidx.compose.runtime.MutableState<TextView?>,
    modifier: Modifier = Modifier,
) {
    val bgColor = MaterialTheme.colorScheme.background.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val density = ctx.resources.displayMetrics.density
            val horizontalPad = (20 * density).toInt()
            val topPad = (24 * density).toInt()
            val bottomPad = (140 * density).toInt()

            val tv = TextView(ctx)
            tv.textSize = 16f
            tv.setLineSpacing(0f, 1.3f)
            tv.setPadding(horizontalPad, topPad, horizontalPad, bottomPad)
            tv.setTextIsSelectable(true)
            tv.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            tv.setOnTouchListener(LinkTapWatcher())

            val sv = ScrollView(ctx).apply { addView(tv) }
            scrollRef.value = sv
            textRef.value = tv
            sv
        },
        update = { scroll ->
            val tv = scroll.getChildAt(0) as TextView
            scrollRef.value = scroll
            textRef.value = tv
            tv.setTextColor(textColor)
            tv.setLinkTextColor(linkColor)
            tv.setBackgroundColor(bgColor)
            markwon.setMarkdown(tv, markdown)
        },
    )
}

@Composable
private fun PlainCodeBody(content: String, modifier: Modifier = Modifier) {
    val bgColor = MaterialTheme.colorScheme.surface.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val density = ctx.resources.displayMetrics.density
            val horizontalPad = (14 * density).toInt()
            val topPad = (16 * density).toInt()
            val bottomPad = (96 * density).toInt()

            val tv = TextView(ctx)
            tv.typeface = android.graphics.Typeface.MONOSPACE
            tv.textSize = 14f
            tv.setLineSpacing(0f, 1.2f)
            tv.setPadding(horizontalPad, topPad, horizontalPad, bottomPad)
            tv.setTextIsSelectable(true)
            tv.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            ScrollView(ctx).apply { addView(tv) }
        },
        update = { scroll ->
            val tv = scroll.getChildAt(0) as TextView
            tv.setTextColor(textColor)
            tv.setBackgroundColor(bgColor)
            tv.text = content.ifEmpty { "（空文件）" }
        },
    )
}

/** 通过标题文本定位并滚动到该标题。 */
private fun jumpToHeading(
    tv: TextView?,
    scroll: ScrollView?,
    entries: List<TocEntry>,
    targetTitle: String,
) {
    if (tv == null || scroll == null) return
    val layout = tv.layout ?: return
    val plain = tv.text?.toString().orEmpty()
    val lines = plain.split('\n')

    fun normalized(s: String): String = s.trim().lowercase()
        .replace(Regex("!\\[[^\\]]*\\]\\([^)]*\\)"), "")
        .replace(Regex("\\[([^\\]]*)\\]\\([^)]*\\)"), "$1")
        .replace(Regex("[`*_~#]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    val yFor = IntArray(entries.size) { -1 }
    var linePtr = 0
    var charOff = 0
    entries.forEachIndexed { idx, entry ->
        val wanted = normalized(entry.title)
        while (linePtr < lines.size) {
            if (normalized(lines[linePtr]) == wanted) {
                val line = layout.getLineForOffset(charOff.coerceIn(0, tv.length()))
                yFor[idx] = tv.paddingTop + layout.getLineTop(line)
                charOff += lines[linePtr].length + 1
                linePtr++
                break
            }
            charOff += lines[linePtr].length + 1
            linePtr++
        }
    }
    val idx = entries.indexOfFirst { it.title == targetTitle }
    val y = if (idx >= 0) yFor[idx] else -1
    if (y >= 0) scroll.smoothScrollTo(0, y)
}

internal fun parseToc(md: String): List<TocEntry> {
    val re = Regex("""^(#{1,6})\s+(.+?)\s*#*\s*$""", RegexOption.MULTILINE)
    return re.findAll(md).map { m ->
        TocEntry(
            level = m.groupValues[1].length,
            title = m.groupValues[2].trim(),
            lineIndex = md.substring(0, m.range.first).count { it == '\n' },
        )
    }.toList()
}

internal fun taskStats(md: String): Pair<Int, Int> {
    val re = Regex("""^\s*[-*]\s+\[([ xX])\]""", RegexOption.MULTILINE)
    var total = 0
    var done = 0
    for (m in re.findAll(md)) {
        total++
        if (m.groupValues[1] != " ") done++
    }
    return done to total
}

/** 处理点击到的链接字符串：路由到对应打开方式。 */
private fun handleInternalLink(
    link: String,
    context: android.content.Context,
    repoDir: File,
    repoId: Long,
    relPath: String,
    onOpenDoc: (Long, String) -> Unit,
    onOpenMermaid: (Long, String, Int) -> Unit,
) {
    when {
        link.startsWith("#mermaid-") -> {
            link.substringAfter("mermaid-").toIntOrNull()?.let { onOpenMermaid(repoId, relPath, it) }
        }
        link.startsWith("http://") || link.startsWith("https://") -> {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            }
        }
        link.startsWith("mailto:") -> {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            }
        }
        link.startsWith("file://") -> {
            val file = runCatching { File(Uri.parse(link).path ?: return@runCatching null) }.getOrNull()
                ?: return
            val repoAbs = repoDir.absolutePath
            val fileAbs = file.absolutePath
            if (fileAbs.startsWith(repoAbs + File.separator)) {
                val rel = fileAbs.removePrefix(repoAbs + File.separator).replace('\\', '/')
                onOpenDoc(repoId, rel)
            }
        }
        else -> Unit
    }
}
