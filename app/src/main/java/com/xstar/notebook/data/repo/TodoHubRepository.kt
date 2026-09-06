package com.xstar.notebook.data.repo

import com.xstar.notebook.data.db.XstarDatabase
import com.xstar.notebook.data.db.entity.LocalTodoEntity
import com.xstar.notebook.data.git.GitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.eclipse.jgit.util.FileUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 独立于 Git 的本地 TODO 清单，以及将清单以 Markdown 导入/导出到某个仓库目录。 */
class TodoHubRepository(
    private val db: XstarDatabase,
    private val git: GitClient,
) {
    private val dao = db.localTodoDao()

    fun observeTodos(): Flow<List<LocalTodoEntity>> = dao.observeAll()

    suspend fun add(title: String, note: String) {
        val now = System.currentTimeMillis()
        dao.insert(
            LocalTodoEntity(
                title = title,
                note = note,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun update(id: Long, title: String, note: String) {
        val item = dao.getAll().find { it.id == id } ?: return
        dao.update(item.copy(title = title, note = note, updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggle(id: Long, done: Boolean) {
        val item = dao.getAll().find { it.id == id } ?: return
        dao.update(item.copy(done = done, updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    private suspend fun snapshot(): List<LocalTodoEntity> = dao.getAll()

    suspend fun listMarkdownFiles(repoId: Long): List<String> = withContext(Dispatchers.IO) {
        val root = git.localDirFor(repoId)
        if (!root.exists()) return@withContext emptyList()
        root.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in setOf("md", "markdown") }
            .map { it.relativeTo(root).invariantSeparatorsPath }
            .sorted()
            .toList()
    }

    suspend fun readFile(repoId: Long, relPath: String): String = withContext(Dispatchers.IO) {
        File(git.localDirFor(repoId), relPath).readText(Charsets.UTF_8)
    }

    /** 将当前本地清单写成 Markdown 并提交到指定仓库目录（不推送）。 */
    suspend fun exportToRepo(repoId: Long, folder: String, fileName: String) {
        val items = snapshot()
        val relPath = sanitize(joinPath(folder, fileName))
        val content = buildString {
            appendLine("# TODO 导出")
            appendLine()
            appendLine("> 由 XStar 笔记导出于 ${timestamp()}，共 ${items.size} 项")
            appendLine()
            items.forEach { item ->
                appendLine("- [${if (item.done) "x" else " "}] ${item.title}")
                if (item.note.isNotBlank()) {
                    item.note.lines().forEach { appendLine("    $it") }
                }
            }
        }
        withContext(Dispatchers.IO) {
            val target = File(git.localDirFor(repoId), relPath)
            FileUtils.mkdirs(target.parentFile ?: git.localDirFor(repoId), true)
            target.writeText(content, Charsets.UTF_8)
        }
        git.commit(repoId, "todos: export $fileName", listOf(relPath))
    }

    /** 解析仓库中的 Markdown，把其中的任务并入本地清单。返回新增条数。 */
    suspend fun importFromRepo(repoId: Long, relPath: String): Int {
        val text = readFile(repoId, relPath)
        val parsed = parseTasks(text)
        if (parsed.isEmpty()) return 0
        val existing = snapshot().map { it.title }.toSet()
        val now = System.currentTimeMillis()
        var added = 0
        parsed.forEach { (title, done) ->
            if (title !in existing) {
                dao.insert(
                    LocalTodoEntity(
                        title = title,
                        done = done,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                added++
            }
        }
        return added
    }

    companion object {
        val taskRegex = Regex("""^\s*[-*+]\s+\[([ xX])\]\s+(.+)$""")

        fun parseTasks(text: String): List<Pair<String, Boolean>> =
            text.lines().mapNotNull { line ->
                val m = taskRegex.matchEntire(line) ?: return@mapNotNull null
                (m.groupValues[2].trim()) to (m.groupValues[1] != " ")
            }

        fun timestamp(): String =
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        fun defaultFileName(): String =
            SimpleDateFormat("'todos-'yyyyMMdd-HHmm'.md'", Locale.getDefault()).format(Date())

        private fun sanitize(relPath: String): String =
            relPath.replace("\\", "/").replace("../", "").removePrefix("/").removePrefix("./")

        private fun joinPath(folder: String, fileName: String): String {
            val f = folder.trim().replace("\\", "/").trim('/')
            return if (f.isBlank()) fileName else "$f/$fileName"
        }
    }
}
