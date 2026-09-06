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

data class MermaidUiState(
    val code: String = "",
    val loading: Boolean = true,
    val error: String? = null,
)

class MermaidViewModel(
    private val repoId: Long,
    private val relPath: String,
    private val blockIndex: Int,
    private val repoRepository: RepoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MermaidUiState())
    val state: StateFlow<MermaidUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val code = withContext(Dispatchers.IO) {
                runCatching {
                    val text = repoRepository.readMarkdownEncoded(repoId, relPath, null)
                    extractMermaidBlock(text, blockIndex) ?: error("找不到该 mermaid 代码块")
                }.getOrElse { e ->
                    _state.update { it.copy(error = e.message) }
                    ""
                }
            }
            _state.update { it.copy(loading = false, code = code) }
        }
    }

    companion object {
        /** 提取第 index 个 ```mermaid 代码块内容。 */
        fun extractMermaidBlock(md: String, index: Int): String? {
            val lines = md.split("\n")
            var i = 0
            var found = -1
            val buf = StringBuilder()
            while (i < lines.size) {
                val t = lines[i].trimStart()
                val m = Regex("^(`{3,}|~{3,})\\s*").find(t)
                if (m != null) {
                    val fenceChar = m.groupValues[1].first().toString()
                    val len = m.groupValues[1].length
                    val lang = t.substring(m.range.last + 1).trim()
                    if (lang == "mermaid") {
                        found++
                        if (found == index) {
                            i++
                            while (i < lines.size) {
                                val cl = lines[i].trimStart()
                                val cm = Regex("^(`{3,}|~{3,})").find(cl)
                                if (cm != null && cm.groupValues[1].first().toString() == fenceChar &&
                                    cm.groupValues[1].length >= len
                                ) {
                                    return buf.toString().trim('\n')
                                }
                                buf.append(lines[i]).append('\n')
                                i++
                            }
                            return buf.toString().trim('\n')
                        }
                    }
                }
                i++
            }
            return null
        }
    }
}
