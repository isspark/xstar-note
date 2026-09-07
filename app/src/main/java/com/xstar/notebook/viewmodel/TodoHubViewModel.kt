package com.xstar.notebook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xstar.notebook.data.db.entity.CategorySystemEntity
import com.xstar.notebook.data.db.entity.RepoEntity
import com.xstar.notebook.data.db.entity.TodoNodeEntity
import com.xstar.notebook.data.repo.RepoRepository
import com.xstar.notebook.data.repo.TodoHubData
import com.xstar.notebook.data.repo.TodoHubRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TodoViewMode(val label: String) {
    CATEGORY("按分类"),
    CALENDAR("日历"),
}

data class TodoHubUiState(
    val data: TodoHubData = TodoHubData(),
    val repos: List<RepoEntity> = emptyList(),
    val selectedRepoId: Long? = null,
    val repoFiles: List<String> = emptyList(),
    val viewMode: TodoViewMode = TodoViewMode.CATEGORY,
    val doneFilter: TodoDoneFilter = TodoDoneFilter.OPEN,
    val selectedSystemId: Long? = null,
    /** null = 展示当前系统的所有分类；非 null = 仅看一个分类。 */
    val selectedCategoryId: Long? = null,
    val busy: Boolean = false,
    val message: String? = null,
) {
    val enabledSystems: List<CategorySystemEntity>
        get() = data.enabledSystems

    val activeSystem: CategorySystemEntity?
        get() = enabledSystems.firstOrNull { it.id == selectedSystemId } ?: enabledSystems.firstOrNull()
}

class TodoHubViewModel(
    private val repoRepository: RepoRepository,
    private val todoHubRepository: TodoHubRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TodoHubUiState())
    val state: StateFlow<TodoHubUiState> = _state.asStateFlow()

    private val dataFlow = todoHubRepository.observeData()
        .stateIn(viewModelScope, SharingStarted.Eagerly, TodoHubData())
    private val reposFlow = repoRepository.observeRepos()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            combine(dataFlow, reposFlow) { data, repos -> data to repos }
                .collect { (data, repos) ->
                    _state.update { current ->
                        val systemId = current.selectedSystemId
                            ?.takeIf { id -> data.systems.any { it.id == id && it.enabled } }
                            ?: data.enabledSystems.firstOrNull()?.id
                        val categoryId = current.selectedCategoryId
                            ?.takeIf { id -> data.categories.any { it.id == id && it.systemId == systemId } }
                        current.copy(
                            data = data,
                            repos = repos,
                            selectedRepoId = current.selectedRepoId
                                ?.takeIf { id -> repos.any { it.id == id } }
                                ?: repos.firstOrNull()?.id,
                            selectedSystemId = systemId,
                            selectedCategoryId = categoryId,
                        )
                    }
                }
        }
    }

    fun setViewMode(mode: TodoViewMode) {
        _state.update { it.copy(viewMode = mode) }
    }

    fun setDoneFilter(filter: TodoDoneFilter) {
        _state.update { it.copy(doneFilter = filter) }
    }

    fun selectSystem(systemId: Long) {
        _state.update { it.copy(selectedSystemId = systemId, selectedCategoryId = null) }
    }

    fun selectCategory(categoryId: Long?) {
        _state.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun applyFilter(systemId: Long?, categoryId: Long?, filter: TodoDoneFilter) {
        _state.update {
            it.copy(
                selectedSystemId = systemId,
                selectedCategoryId = categoryId,
                doneFilter = filter,
            )
        }
    }

    fun addNode(title: String, note: String, startAt: Long?, dueAt: Long?, tags: Set<Long>) {
        if (title.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val id = todoHubRepository.addNode(title, note, startAt, dueAt)
                if (tags.isNotEmpty()) todoHubRepository.setTags(id, tags)
            }.onFailure { error ->
                _state.update { it.copy(message = "保存失败：" + friendly(error)) }
            }
        }
    }

    fun updateNode(
        item: TodoNodeEntity,
        title: String,
        note: String,
        startAt: Long?,
        dueAt: Long?,
        tags: Set<Long>,
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            runCatching {
                todoHubRepository.updateNode(item.id, title, note, startAt, dueAt)
                todoHubRepository.setTags(item.id, tags)
            }.onFailure { error ->
                _state.update { it.copy(message = "保存失败：" + friendly(error)) }
            }
        }
    }

    fun toggle(item: TodoNodeEntity) {
        viewModelScope.launch {
            runCatching { todoHubRepository.toggle(item.id, !item.done) }
                .onFailure { error ->
                    _state.update { it.copy(message = "更新失败：" + friendly(error)) }
                }
        }
    }

    fun delete(item: TodoNodeEntity) {
        viewModelScope.launch {
            runCatching { todoHubRepository.delete(item.id) }
                .onSuccess { _state.update { it.copy(message = "已删除「${item.title.take(20)}」") } }
                .onFailure { error ->
                    _state.update { it.copy(message = "删除失败：" + friendly(error)) }
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

    fun exportToRepo(repoId: Long, folder: String) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            runCatching {
                todoHubRepository.exportToRepo(repoId, folder, TodoHubRepository.defaultFileName())
            }.onSuccess {
                val cleanFolder = folder.trim().replace("\\", "/").trim('/')
                _state.update {
                    it.copy(
                        busy = false,
                        message = if (cleanFolder.isBlank()) {
                            "已导出并提交到仓库根目录（可去仓库管理推送）"
                        } else {
                            "已导出到 $cleanFolder/ 并提交（可去仓库管理推送）"
                        },
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(busy = false, message = "导出失败：" + friendly(error)) }
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
                .onFailure { error ->
                    _state.update { it.copy(busy = false, message = "导入失败：" + friendly(error)) }
                }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun friendly(error: Throwable): String =
        error.message?.take(160).orEmpty().ifEmpty { error::class.java.simpleName }
}
