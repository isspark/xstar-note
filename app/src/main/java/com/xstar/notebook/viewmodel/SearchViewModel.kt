package com.xstar.notebook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xstar.notebook.data.repo.RepoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(private val repoRepository: RepoRepository) : ViewModel() {

    private val _results = MutableStateFlow<List<RepoRepository.SearchHit>>(emptyList())
    val results: StateFlow<List<RepoRepository.SearchHit>> = _results.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    private var job: Job? = null

    fun search(query: String) {
        job?.cancel()
        val q = query.trim()
        if (q.isEmpty()) {
            _searching.value = false
            _results.value = emptyList()
            return
        }
        job = viewModelScope.launch {
            delay(300)
            _searching.value = true
            val hits = runCatching {
                val repos = repoRepository.reposOnce()
                repoRepository.searchGlobal(repos, q)
            }.getOrDefault(emptyList())
            _searching.value = false
            _results.value = hits
        }
    }
}
