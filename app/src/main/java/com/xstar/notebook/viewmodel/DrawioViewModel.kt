package com.xstar.notebook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xstar.notebook.data.git.GitClient
import com.xstar.notebook.data.git.GitOperationException
import com.xstar.notebook.data.repo.RepoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DrawioUiState(
    val xml: String = "",
    val loading: Boolean = true,
    val error: String? = null,
    val fileName: String = "",
)

class DrawioViewModel(
    private val repoId: Long,
    private val relPath: String,
    private val repoRepository: RepoRepository,
    private val gitClient: GitClient,
) : ViewModel() {

    private val _state = MutableStateFlow(DrawioUiState(fileName = relPath.substringAfterLast('/')))
    val state: StateFlow<DrawioUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val xml = withContext(Dispatchers.IO) {
                runCatching { readXml() }
                    .getOrElse { e -> _state.update { it.copy(error = e.message) }; "" }
            }
            _state.update { it.copy(loading = false, xml = xml) }
        }
    }

    private suspend fun readXml(): String {
        val file = gitClient.absolutePath(repoId, relPath)
        if (!file.exists()) throw GitOperationException("文件不存在")
        if (file.length() > 5 * 1024 * 1024) {
            throw GitOperationException("文件超过 5MB，离线预览过于吃力，可先“用文本方式查看”或分享原文件")
        }
        val raw = file.readBytes().let { String(it, Charsets.UTF_8) }.trim()
        if (raw.isEmpty()) throw GitOperationException("文件为空")
        return raw
    }
}
