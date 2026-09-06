package com.xstar.notebook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xstar.notebook.data.db.entity.TodoEntity
import com.xstar.notebook.data.repo.RepoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoListViewModel(
    private val repoId: Long,
    private val relPath: String,
    private val repoRepository: RepoRepository,
) : ViewModel() {

    val todos: StateFlow<List<TodoEntity>> = repoRepository.observeTodosByDoc(repoId, relPath)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun toggle(todo: TodoEntity, checked: Boolean) {
        viewModelScope.launch {
            val repo = repoRepository.getRepo(repoId) ?: return@launch
            _busy.value = true
            runCatching { repoRepository.toggleTodo(repo, todo, checked) }
                .onFailure { _message.value = "更新失败：" + (it.message?.take(150).orEmpty()) }
            _busy.value = false
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}