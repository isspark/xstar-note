package com.xstar.notebook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xstar.notebook.data.db.entity.RepoEntity
import com.xstar.notebook.data.repo.RepoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class BrowseUiState(
    val currentDir: String = "",
    val pathSegments: List<String> = emptyList(),
    val files: List<File> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

class BrowseViewModel(
    private val repoId: Long,
    private val repoRepository: RepoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BrowseUiState())
    val state: StateFlow<BrowseUiState> = _state.asStateFlow()

    val repo: StateFlow<RepoEntity?> = repoRepository.observeRepo(repoId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        load("")
    }

    fun load(relDir: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val files = withContext(Dispatchers.IO) {
                runCatching { repoRepository.listFiles(repoId, relDir) }
                    .getOrElse { e ->
                        _state.update { it.copy(error = e.message) }
                        emptyList()
                    }
            }
            val segments = if (relDir.isBlank()) emptyList() else relDir.split('/')
            _state.update { it.copy(loading = false, files = files, currentDir = relDir, pathSegments = segments) }
        }
    }

    fun navigate(dir: String) = load(dir)

    /** 离开本页（进入子页面）时置位，返回时自动刷新一次。 */
    fun markNeedsRefresh() {
        needsRefresh = true
    }

    /** 若从子页面返回则重新加载当前目录，避免列表陈旧。 */
    fun refreshIfNeeded() {
        if (needsRefresh) {
            needsRefresh = false
            load(_state.value.currentDir)
        }
    }

    private var needsRefresh: Boolean = false

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    /** 顶部刷新/下拉刷新：拉取远程改动 → 重建索引 → 重载列表，完成后自动结束指示。 */
    suspend fun syncRefresh(): String? {
        if (_syncing.value) return null
        _syncing.value = true
        _state.update { it.copy(loading = true, error = null) }
        var message: String? = null
        try {
            val repo = withContext(Dispatchers.IO) { repoRepository.getRepo(repoId) }
            if (repo == null) {
                message = "仓库不存在或已删除"
            } else {
                val ok = withContext(Dispatchers.IO) {
                    runCatching { repoRepository.pull(repo) }.isSuccess
                }
                message = if (ok) {
                    "已同步到最新"
                } else {
                    val stateNow = withContext(Dispatchers.IO) { repoRepository.getRepo(repoId)?.syncState }
                    if (stateNow == "conflict") "拉取出现冲突，请在“仓库管理”中处理" else "同步失败，请检查网络后重试"
                }
            }
        } finally {
            _syncing.value = false
            load(_state.value.currentDir)
        }
        return message
    }

    /** 重试推送本地未推送的提交。 */
    fun retryPush() {
        viewModelScope.launch {
            val r = repo.value
            if (r != null) {
                repoRepository.retryPush(r)
                load(_state.value.currentDir)
            }
        }
    }
}