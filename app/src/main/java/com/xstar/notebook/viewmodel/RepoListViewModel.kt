package com.xstar.notebook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xstar.notebook.data.db.entity.RepoEntity
import com.xstar.notebook.data.repo.RepoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RepoListViewModel(private val repoRepository: RepoRepository) : ViewModel() {

    val repos: StateFlow<List<RepoEntity>> = repoRepository.observeRepos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _busyIds = MutableStateFlow<Set<Long>>(emptySet())
    val busyIds: StateFlow<Set<Long>> = _busyIds.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun pull(repo: RepoEntity) {
        viewModelScope.launch {
            _busyIds.update { it + repo.id }
            runCatching { repoRepository.pull(repo) }
                .onSuccess { _message.value = "已同步最新内容" }
                .onFailure { _message.value = "拉取失败：" + friendly(it) }
            _busyIds.update { it - repo.id }
        }
    }

    fun push(repo: RepoEntity) {
        viewModelScope.launch {
            _busyIds.update { it + repo.id }
            runCatching { repoRepository.push(repo) }
                .onSuccess { _message.value = "已推送" }
                .onFailure { _message.value = "推送失败：" + friendly(it) }
            _busyIds.update { it - repo.id }
        }
    }

    fun sync(repo: RepoEntity) {
        viewModelScope.launch {
            _busyIds.update { it + repo.id }
            runCatching { repoRepository.sync(repo) }
                .onSuccess { _message.value = "同步完成：已拉取并推送" }
                .onFailure { _message.value = "同步失败：" + friendly(it) }
            _busyIds.update { it - repo.id }
        }
    }

    fun delete(repo: RepoEntity) {
        viewModelScope.launch {
            runCatching { repoRepository.deleteRepo(repo) }
                .onFailure { _message.value = "删除失败：" + friendly(it) }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    private fun friendly(e: Throwable): String {
        val msg = e.message?.take(160).orEmpty()
        return msg.ifEmpty { e::class.java.simpleName }
    }
}