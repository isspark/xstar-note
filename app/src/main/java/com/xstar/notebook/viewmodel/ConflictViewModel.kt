package com.xstar.notebook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xstar.notebook.data.repo.RepoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConflictUiState(
    val loading: Boolean = true,
    val conflicts: List<String> = emptyList(),
    val resolvingPath: String? = null,
)

class ConflictViewModel(
    private val repoId: Long,
    private val repoRepository: RepoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ConflictUiState())
    val state: StateFlow<ConflictUiState> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val conflicts = runCatching { repoRepository.listConflicts(repoId) }.getOrDefault(emptyList())
            _state.update { it.copy(loading = false, conflicts = conflicts) }
        }
    }

    /** 逐个解决冲突文件。 */
    fun resolve(path: String, keepOurs: Boolean) {
        val current = _state.value
        if (current.resolvingPath != null) return
        _state.update { it.copy(resolvingPath = path) }
        viewModelScope.launch {
            runCatching {
                val repo = repoRepository.getRepo(repoId)
                    ?: error("仓库不存在")
                repoRepository.resolveConflict(repo, path, keepOurs)
            }.onSuccess { res ->
                _message.value = if (res.remaining.isEmpty()) {
                    if (res.pushed) "冲突已解决并推送到远程" else "冲突已解决（本地提交，推送失败可在仓库管理重试）"
                } else {
                    "已解决 $path，剩余 ${res.remaining.size} 个冲突"
                }
                _state.update { it.copy(resolvingPath = null, conflicts = res.remaining) }
            }.onFailure {
                _state.update { it.copy(resolvingPath = null) }
                _message.value = "解决失败：" + (it.message?.take(160) ?: "未知错误")
            }
        }
    }

    /** 全部保留某一侧。 */
    fun resolveAll(keepOurs: Boolean) {
        val paths = _state.value.conflicts.toList()
        if (paths.isEmpty() || _state.value.resolvingPath != null) return
        viewModelScope.launch {
            var failed: String? = null
            var remaining = paths.toList()
            for (p in paths) {
                _state.update { it.copy(resolvingPath = p) }
                try {
                    val repo = repoRepository.getRepo(repoId) ?: error("仓库不存在")
                    val res = repoRepository.resolveConflict(repo, p, keepOurs)
                    remaining = res.remaining
                } catch (e: Exception) {
                    failed = e.message?.take(160) ?: "未知错误"
                    break
                }
            }
            _state.update { it.copy(resolvingPath = null, conflicts = remaining) }
            _message.value = when {
                failed != null -> "解决失败：$failed"
                remaining.isEmpty() -> "全部冲突已解决并提交"
                else -> "剩余 ${remaining.size} 个冲突"
            }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
