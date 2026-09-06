package com.xstar.notebook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xstar.notebook.data.repo.RepoRepository
import com.xstar.notebook.domain.model.HostType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddRepoUiState(
    val displayName: String = "",
    val remoteUrl: String = "",
    val hostType: HostType = HostType.GITHUB,
    val username: String = "",
    val token: String = "",
    val branch: String = "main",
    val busy: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
    val addedRepoId: Long? = null,
)

class AddRepoViewModel(private val repoRepository: RepoRepository) : ViewModel() {

    private val _state = MutableStateFlow(AddRepoUiState())
    val state: StateFlow<AddRepoUiState> = _state.asStateFlow()

    fun onDisplayNameChange(v: String) = _state.update { it.copy(displayName = v) }
    fun onRemoteUrlChange(v: String) = _state.update { it.copy(remoteUrl = v) }
    fun onHostTypeChange(v: HostType) = _state.update { it.copy(hostType = v) }
    fun onUsernameChange(v: String) = _state.update { it.copy(username = v) }
    fun onTokenChange(v: String) = _state.update { it.copy(token = v) }
    fun onBranchChange(v: String) = _state.update { it.copy(branch = v) }

    fun submit() {
        val s = _state.value
        if (s.busy) return
        if (s.remoteUrl.isBlank()) return _state.update { it.copy(error = "请填写仓库地址") }
        if (s.token.isBlank()) return _state.update { it.copy(error = "请填写访问令牌 PAT") }
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val name = s.displayName.ifBlank { guessName(s.remoteUrl) }
                repoRepository.addRepo(
                    displayName = name,
                    remoteUrl = s.remoteUrl.trim(),
                    hostType = s.hostType,
                    username = s.username.trim(),
                    token = s.token.trim(),
                    branch = s.branch.ifBlank { "main" },
                )
            }.onSuccess { id ->
                _state.update { it.copy(busy = false, done = true, addedRepoId = id) }
            }.onFailure { e ->
                _state.update { it.copy(busy = false, error = e.message?.take(200).orEmpty()) }
            }
        }
    }

    private fun guessName(url: String): String {
        val trimmed = url.trim().trimEnd('/')
        return trimmed.substringAfterLast('/').ifBlank { "仓库" }
    }
}