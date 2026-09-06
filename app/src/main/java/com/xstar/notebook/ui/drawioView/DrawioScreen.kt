package com.xstar.notebook.ui.drawioView

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xstar.notebook.ui.components.FilledActionButton
import com.xstar.notebook.ui.components.GradientTopBar
import com.xstar.notebook.viewmodel.DrawioViewModel
import java.io.File

@Composable
fun DrawioScreen(
    repoId: Long,
    relPath: String,
    onBack: () -> Unit,
    onOpenRawText: (Long, String) -> Unit = { _, _ -> },
    vm: DrawioViewModel = viewModel {
        val app = this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY] as com.xstar.notebook.XstarApplication
        DrawioViewModel(repoId, relPath, app.container.repoRepository, app.container.gitClient)
    },
) {
    val state by vm.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = (context.applicationContext as com.xstar.notebook.XstarApplication).container
    val originalFile = remember { container.gitClient.absolutePath(repoId, relPath) }
    val webRef = remember { mutableStateOf<WebView?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    fun shareOriginal() {
        runCatching { shareFile(context, originalFile, "application/octet-stream") }
    }

    fun exportPng() {
        webRef.value?.evaluateJavascript("window.exportPng && window.exportPng();", null)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GradientTopBar(
                title = relPath.substringAfterLast('/'),
                subtitle = "drawio 图表预览",
                onBack = onBack,
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "更多操作", tint = androidx.compose.ui.graphics.Color.White)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("导出为图片") },
                                leadingIcon = { Icon(Icons.Rounded.IosShare, null) },
                                onClick = {
                                    menuOpen = false
                                    exportPng()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("分享源文件") },
                                leadingIcon = { Icon(Icons.Rounded.Share, null) },
                                onClick = {
                                    menuOpen = false
                                    shareOriginal()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("用文本方式查看") },
                                leadingIcon = { Icon(Icons.Rounded.Description, null) },
                                onClick = {
                                    menuOpen = false
                                    onOpenRawText(repoId, relPath)
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp),
                ) {
                    Text(
                        "无法离线预览",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                    Text(
                        state.error ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    FilledActionButton(onClick = vm::load) {
                        Text("重试", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                    TextButton(onClick = { onOpenRawText(repoId, relPath) }) {
                        Text("用文本方式查看源文件")
                    }
                }
            }
            else -> DrawioWebView(
                xml = state.xml,
                originalFile = originalFile,
                modifier = Modifier.fillMaxSize().padding(padding),
                onWebView = { webRef.value = it },
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DrawioWebView(
    xml: String,
    originalFile: File,
    modifier: Modifier = Modifier,
    onWebView: (WebView) -> Unit = {},
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val bridge = DrawioShareBridge(ctx, originalFile)
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                addJavascriptInterface(bridge, "Android")
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        injectXml(view, xml)
                    }
                }
                loadUrl("file:///android_asset/drawio/viewer.html")
            }.also { onWebView(it) }
        },
        update = { view ->
            onWebView(view)
            injectXml(view, xml)
        },
    )
}

private fun injectXml(view: WebView, xml: String) {
    val encoded = org.json.JSONObject.quote(xml)
    view.evaluateJavascript(
        "window.raw=$encoded; window.basePath=''; window.renderViewer();",
        null,
    )
}

private class DrawioShareBridge(
    private val context: Context,
    private val originalFile: File,
) {
    @JavascriptInterface
    fun sharePng(dataUrl: String) {
        runCatching {
            val base64 = dataUrl.substringAfter(',')
            val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
            val dir = File(context.cacheDir, "drawio_share").apply { mkdirs() }
            val out = File(dir, originalFile.nameWithoutExtension + ".png")
            out.writeBytes(bytes)
            shareFile(context, out, "image/png")
        }.onFailure {
            Toast.makeText(context, "导出失败：" + (it.message?.take(100) ?: "未知错误"), Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun shareOriginal() {
        shareFile(context, originalFile, "application/octet-stream")
    }
}

private fun shareFile(context: Context, file: File, mime: String) {
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "分享 ${file.name}"))
}
