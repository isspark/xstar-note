package com.xstar.notebook.ui.navigation

object Routes {
    const val REPOS = "repos"
    const val ADD_REPO = "add_repo"
    const val BROWSE = "browse/{repoId}"
    const val MD_VIEW = "md/{repoId}/{relPath}"
    const val CODE_VIEW = "code/{repoId}/{relPath}"
    const val TODO_LIST = "todo/{repoId}/{relPath}"
    const val DRAWIO = "drawio/{repoId}/{relPath}"
    const val TODO_HUB = "todos"
    const val TODO_CLASSIFY = "todos/classify"
    const val TODO_EDITOR = "todos/edit/{todoId}"
    const val HOME_BOTH = "home/{repoId}/{todoFirst}"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
    const val CONFLICTS = "conflicts/{repoId}"
    const val MERMAID = "mermaid/{repoId}/{relPath}/{blockIndex}"

    fun browse(repoId: Long) = "browse/$repoId"
    fun mdView(repoId: Long, relPath: String) = "md/$repoId/${encode(relPath)}"
    fun codeView(repoId: Long, relPath: String) = "code/$repoId/${encode(relPath)}"
    fun todoList(repoId: Long, relPath: String) = "todo/$repoId/${encode(relPath)}"
    fun todoEditor(todoId: Long = 0L) = "todos/edit/$todoId"
    fun homeBoth(repoId: Long, todoFirst: Boolean) = "home/$repoId/${if (todoFirst) 1 else 0}"
    fun drawio(repoId: Long, relPath: String) = "drawio/$repoId/${encode(relPath)}"
    fun conflicts(repoId: Long) = "conflicts/$repoId"
    fun mermaid(repoId: Long, relPath: String, blockIndex: Int) =
        "mermaid/$repoId/${encode(relPath)}/$blockIndex"

    private fun encode(s: String) = android.net.Uri.encode(s)
    fun decode(s: String) = android.net.Uri.decode(s) ?: s
}
