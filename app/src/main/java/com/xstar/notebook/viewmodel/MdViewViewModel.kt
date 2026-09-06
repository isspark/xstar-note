package com.xstar.notebook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xstar.notebook.data.repo.RepoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MdViewUiState(
    val content: String = "",
    val loading: Boolean = true,
    val error: String? = null,
    val saving: Boolean = false,
    val charsetName: String? = null,
    val backlinks: List<String> = emptyList(),
    val backlinksLoading: Boolean = false,
    val repoName: String = "",
)

class MdViewViewModel(
    private val repoId: Long,
    private val relPath: String,
    private val repoRepository: RepoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MdViewUiState())
    val state: StateFlow<MdViewUiState> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** 本次编辑会话中插入的本地资产（图片等），保存时一并纳入提交。 */
    private val pendingAssets = mutableListOf<String>()

    init {
        load()
        loadRepoName()
    }

    private fun loadRepoName() {
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) { repoRepository.getRepo(repoId)?.displayName.orEmpty() }
            _state.update { it.copy(repoName = name) }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val content = withContext(Dispatchers.IO) {
                runCatching { repoRepository.readMarkdown(repoId, relPath) }
                    .getOrElse { e -> _state.update { it.copy(error = e.message) }; "" }
            }
            _state.update { it.copy(loading = false, content = content) }
        }
    }

    fun loadWithCharset(charsetName: String?) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val content = withContext(Dispatchers.IO) {
                runCatching { repoRepository.readMarkdownEncoded(repoId, relPath, charsetName) }
                    .getOrElse { e -> _state.update { it.copy(error = e.message) }; "" }
            }
            _state.update { it.copy(loading = false, content = content, charsetName = charsetName) }
        }
    }

    /** 把字节写入仓库目录（不提交），返回仓库内相对路径，供编辑器插入引用。 */
    suspend fun importAssetImage(folder: String, bytes: ByteArray, ext: String): String? =
        withContext(Dispatchers.IO) {
            val repo = repoRepository.getRepo(repoId) ?: return@withContext null
            val rel = runCatching { repoRepository.importImageBytes(repo, folder, bytes, ext) }
                .getOrNull() ?: return@withContext null
            pendingAssets.add(rel)
            rel
        }

    /** 保存文档（含本次插入的图片资产）。 */
    fun save(content: String, onDraftSaved: () -> Unit = {}) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result = runCatching {
                val repo = repoRepository.getRepo(repoId)
                    ?: error("仓库不存在或已删除")
                val referenced = pendingAssets.filter { rel ->
                    val name = rel.substringAfterLast('/')
                    content.contains(name)
                }
                repoRepository.updateMarkdown(repo, relPath, content, true, referenced)
            }
            result
                .onSuccess { pushed ->
                    pendingAssets.clear()
                    onDraftSaved()
                    _message.value = if (pushed) {
                        "已保存并同步到 Git"
                    } else {
                        "已保存到本地并提交（未能推送，稍后可在仓库管理同步）"
                    }
                }
                .onFailure {
                    _message.value = "保存失败：" + (it.message?.take(160) ?: "未知错误")
                }
            _state.update { it.copy(saving = false) }
            load()
        }
    }

    fun loadBacklinks() {
        if (_state.value.backlinksLoading) return
        _state.update { it.copy(backlinksLoading = true) }
        viewModelScope.launch {
            val links = withContext(Dispatchers.IO) {
                runCatching { repoRepository.findBacklinks(repoId, relPath) }.getOrDefault(emptyList())
            }
            _state.update { it.copy(backlinks = links, backlinksLoading = false) }
        }
    }

    /** 放弃本次编辑时删除已写入但未提交的图片资产。 */
    suspend fun discardAssets() = withContext(Dispatchers.IO) {
        if (pendingAssets.isEmpty()) return@withContext
        val repo = repoRepository.getRepo(repoId)
        if (repo != null) {
            pendingAssets.forEach { rel ->
                runCatching { repoRepository.removeLocalFile(repo.id, rel) }
            }
        }
        pendingAssets.clear()
    }

    fun consumeMessage() {
        _message.value = null
    }
}
