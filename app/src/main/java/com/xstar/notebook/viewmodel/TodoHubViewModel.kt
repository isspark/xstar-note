package com.xstar.notebook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xstar.notebook.data.db.entity.LocalTodoEntity
import com.xstar.notebook.data.db.entity.RepoEntity
import com.xstar.notebook.data.repo.RepoRepository
import com.xstar.notebook.data.repo.TodoHubRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodoHubUiState(
    val todos: List<LocalTodoEntity> = emptyList(),
    val repos: List<RepoEntity> = emptyList(),
    val repoFiles: List<String> = emptyList(),
    val selectedRepoId: Long? = null,
    val busy: Boolean = false,
    val message: String? = null,
)

class TodoHubViewModel(
    private val repoRepository: RepoRepository,
    private val todoHubRepository: TodoHubRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TodoHubUiState())
    val state: StateFlow<TodoHubUiState> = _state.asStateFlow()

    private val todosFlow = todoHubRepository.observeTodos()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val reposFlow = repoRepository.observeRepos()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            combine(todosFlow, reposFlow) { todos, repos -> todos to repos }
                .collect { (todos, repos) ->
                    _state.update { s ->
                        val keep = s.selectedRepoId?.takeIf { id -> repos.any { it.id == id } }
                        s.copy(
                            todos = todos,
                            repos = repos,
                            selectedRepoId = keep ?: repos.firstOrNull()?.id,
                        )
                    }
                }
        }
    }

    fun selectRepo(id: Long) {
        _state.update { it.copy(selectedRepoId = id) }
        refreshFiles(id)
    }

    fun refreshFiles(repoId: Long) {
        viewModelScope.launch {
            val files = runCatching { todoHubRepository.listMarkdownFiles(repoId) }
                .getOrElse { emptyList() }
            _state.update { it.copy(repoFiles = files) }
        }
    }

    fun add(title: String, note: String) {
        viewModelScope.launch {
            runCatching { todoHubRepository.add(title, note) }
                .onFailure { e -> _state.update { it.copy(message = "保存失败：" + friendly(e)) } }
        }
    }

    fun update(item: LocalTodoEntity, title: String, note: String) {
        viewModelScope.launch {
            runCatching { todoHubRepository.update(item.id, title, note) }
                .onFailure { e -> _state.update { it.copy(message = "保存失败：" + friendly(e)) } }
        }
    }

    fun toggle(item: LocalTodoEntity) {
        viewModelScope.launch {
            runCatching { todoHubRepository.toggle(item.id, !item.done) }
                .onFailure { e -> _state.update { it.copy(message = "更新失败：" + friendly(e)) } }
        }
    }

    fun delete(item: LocalTodoEntity) {
        viewModelScope.launch {
            runCatching { todoHubRepository.delete(item.id) }
                .onFailure { e -> _state.update { it.copy(message = "删除失败：" + friendly(e)) } }
        }
    }

    fun exportToRepo(repoId: Long, folder: String) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            runCatching { todoHubRepository.exportToRepo(repoId, folder, TodoHubRepository.defaultFileName()) }
                .onSuccess {
                    val f = folder.trim().replace("\\", "/").trim('/')
                    _state.update {
                        it.copy(
                            busy = false,
                            message = if (f.isBlank()) {
                                "已导出并提交到仓库根目录（可去仓库管理推送）"
                            } else {
                                "已导出到 $f/ 并提交（可去仓库管理推送）"
                            },
                        )
                    }
                }
                .onFailure {
                    _state.update { s -> s.copy(busy = false, message = "导出失败：" + friendly(it)) }
                }
        }
    }

    fun importFile(repoId: Long, relPath: String) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            runCatching { todoHubRepository.importFromRepo(repoId, relPath) }
                .onSuccess { count ->
                    _state.update {
                        it.copy(
                            busy = false,
                            message = if (count > 0) "已导入 $count 条任务" else "没有需要导入的新任务",
                        )
                    }
                }
                .onFailure {
                    _state.update { s -> s.copy(busy = false, message = "导入失败：" + friendly(it)) }
                }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun friendly(e: Throwable): String {
        val msg = e.message?.take(160).orEmpty()
        return msg.ifEmpty { e::class.java.simpleName }
    }
}
