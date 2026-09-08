package com.xstar.notebook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xstar.notebook.data.db.entity.InboxItemEntity
import com.xstar.notebook.data.repo.InboxRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InboxViewModel(private val repository: InboxRepository) : ViewModel() {
    val items: StateFlow<List<InboxItemEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(type: String, title: String, content: String, url: String?, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.add(type, title, content, url) }.onSuccess { onSaved() }
        }
    }

    fun update(item: InboxItemEntity, title: String, content: String, url: String?, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.update(item, title, content, url) }.onSuccess { onSaved() }
        }
    }

    fun archive(item: InboxItemEntity) = viewModelScope.launch { repository.archive(item) }

    fun delete(item: InboxItemEntity) = viewModelScope.launch { repository.delete(item) }

    fun convertToTodo(item: InboxItemEntity, onDone: (Long) -> Unit) = viewModelScope.launch {
        runCatching { repository.convertToTodo(item) }.onSuccess(onDone)
    }
}
