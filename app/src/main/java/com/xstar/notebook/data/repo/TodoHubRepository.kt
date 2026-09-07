package com.xstar.notebook.data.repo

import androidx.room.withTransaction
import com.xstar.notebook.data.db.XstarDatabase
import com.xstar.notebook.data.db.entity.CategoryEntity
import com.xstar.notebook.data.db.entity.CategorySystemEntity
import com.xstar.notebook.data.db.entity.NodeCategoryEntity
import com.xstar.notebook.data.db.entity.TodoNodeEntity
import com.xstar.notebook.data.git.GitClient
import com.xstar.notebook.data.todo.TodoPresets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.eclipse.jgit.util.FileUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TodoHubData(
    val nodes: List<TodoNodeEntity> = emptyList(),
    val systems: List<CategorySystemEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val links: List<NodeCategoryEntity> = emptyList(),
) {
    val enabledSystems: List<CategorySystemEntity>
        get() = systems.filter { it.enabled }.sortedWith(compareBy({ it.position }, { it.id }))

    fun categoriesOf(systemId: Long): List<CategoryEntity> =
        categories.filter { it.systemId == systemId }.sortedWith(compareBy({ it.position }, { it.id }))
}

/** 本地扁平 TODO、分类标签，以及 Markdown 导入导出。 */
class TodoHubRepository(
    private val db: XstarDatabase,
    private val git: GitClient,
) {
    private val nodeDao = db.todoNodeDao()
    private val categoryDao = db.categoryDao()

    fun observeData(): Flow<TodoHubData> =
        combine(
            nodeDao.observeAll(),
            categoryDao.observeSystems(),
            categoryDao.observeCategories(),
            categoryDao.observeNodeCategories(),
        ) { nodes, systems, categories, links ->
            TodoHubData(nodes, systems, categories, links)
        }

    suspend fun addNode(
        title: String,
        note: String = "",
        startAt: Long? = null,
        dueAt: Long? = null,
    ): Long {
        val now = System.currentTimeMillis()
        return nodeDao.insert(
            TodoNodeEntity(
                title = title.trim(),
                note = note.trim(),
                startAt = startAt,
                dueAt = dueAt,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun updateNode(id: Long, title: String, note: String, startAt: Long?, dueAt: Long?) {
        val item = nodeDao.getById(id) ?: return
        nodeDao.update(
            item.copy(
                title = title.trim(),
                note = note.trim(),
                startAt = startAt,
                dueAt = dueAt,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun toggle(id: Long, done: Boolean): Boolean {
        val item = nodeDao.getById(id) ?: return false
        nodeDao.update(item.copy(done = done, updatedAt = System.currentTimeMillis()))
        return true
    }

    suspend fun delete(id: Long) = db.withTransaction {
        categoryDao.deleteNodeCategories(listOf(id))
        nodeDao.deleteById(id)
    }

    suspend fun setTags(nodeId: Long, categoryIds: Set<Long>) = db.withTransaction {
        val current = categoryDao.getAllNodeCategories().filter { it.nodeId == nodeId }
        val existing = current.mapTo(HashSet()) { it.categoryId }
        current.filter { it.categoryId !in categoryIds }
            .forEach { categoryDao.deleteNodeCategory(it.nodeId, it.categoryId) }
        (categoryIds - existing).forEach {
            categoryDao.insertNodeCategory(NodeCategoryEntity(nodeId, it))
        }
    }

    suspend fun addCustomSystem(name: String, categoryNames: List<String>): Long {
        val names = categoryNames.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        var systemId = 0L
        db.withTransaction {
            val position = (categoryDao.getAllSystems().maxOfOrNull { it.position } ?: -1) + 1
            systemId = categoryDao.insertSystem(
                CategorySystemEntity(
                    name = name.trim().ifEmpty { "自定义分类" },
                    enabled = true,
                    position = position,
                ),
            )
            names.forEachIndexed { index, categoryName ->
                categoryDao.insertCategory(
                    CategoryEntity(
                        systemId = systemId,
                        name = categoryName,
                        colorArgb = TodoPresets.PALETTE[index % TodoPresets.PALETTE.size],
                        position = index,
                    ),
                )
            }
        }
        return systemId
    }

    suspend fun renameSystem(id: Long, name: String) {
        val system = categoryDao.getSystemById(id) ?: return
        categoryDao.updateSystem(system.copy(name = name.trim().ifEmpty { system.name }))
    }

    suspend fun setSystemEnabled(id: Long, enabled: Boolean) {
        val system = categoryDao.getSystemById(id) ?: return
        categoryDao.updateSystem(system.copy(enabled = enabled))
    }

    suspend fun deleteSystem(id: Long) {
        val system = categoryDao.getSystemById(id) ?: return
        require(system.builtInKey == null) { "内置系统不可删除，可改为停用" }
        db.withTransaction {
            val categories = categoryDao.getCategoriesBySystem(id)
            categories.forEach { categoryDao.deleteNodeCategoriesByCategory(it.id) }
            categoryDao.deleteCategoriesBySystem(id)
            categoryDao.deleteSystemById(id)
        }
    }

    suspend fun addCategory(systemId: Long, name: String) {
        val existing = categoryDao.getCategoriesBySystem(systemId)
        categoryDao.insertCategory(
            CategoryEntity(
                systemId = systemId,
                name = name.trim(),
                colorArgb = TodoPresets.PALETTE[existing.size % TodoPresets.PALETTE.size],
                position = existing.size,
            ),
        )
    }

    suspend fun renameCategory(id: Long, name: String) {
        val category = categoryDao.getCategoryById(id) ?: return
        categoryDao.updateCategory(category.copy(name = name.trim().ifEmpty { category.name }))
    }

    suspend fun recolorCategory(id: Long, colorArgb: Long) {
        val category = categoryDao.getCategoryById(id) ?: return
        categoryDao.updateCategory(category.copy(colorArgb = colorArgb))
    }

    suspend fun deleteCategory(id: Long) = db.withTransaction {
        categoryDao.deleteNodeCategoriesByCategory(id)
        categoryDao.deleteCategoryById(id)
    }

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

    suspend fun exportToRepo(repoId: Long, folder: String, fileName: String) {
        val items = nodeDao.getAll().sortedWith(todoOrder)
        val relPath = sanitize(joinPath(folder, fileName))
        val content = buildString {
            appendLine("# TODO 导出")
            appendLine()
            appendLine("> 由 XStar 笔记导出于 ${timestamp()}，共 ${items.size} 项")
            appendLine()
            items.forEach { item ->
                val start = item.startAt?.let { " <!-- xstar-start:$it -->" }.orEmpty()
                val due = item.dueAt?.let { " <!-- xstar-due:$it -->" }.orEmpty()
                appendLine("- [${if (item.done) "x" else " "}] ${item.title}$start$due")
                item.note.lines().filter { it.isNotBlank() }.forEach { appendLine("    $it") }
            }
        }
        withContext(Dispatchers.IO) {
            val target = File(git.localDirFor(repoId), relPath)
            FileUtils.mkdirs(target.parentFile ?: git.localDirFor(repoId))
            target.writeText(content, Charsets.UTF_8)
        }
        git.commit(repoId, "todos: export $fileName", listOf(relPath))
    }

    suspend fun importFromRepo(repoId: Long, relPath: String): Int {
        val parsed = parseTasks(readFile(repoId, relPath))
        if (parsed.isEmpty()) return 0
        val existing = nodeDao.getAll().mapTo(HashSet()) { it.title }
        val now = System.currentTimeMillis()
        var count = 0
        parsed.forEach { task ->
            if (task.title !in existing) {
                nodeDao.insert(
                    TodoNodeEntity(
                        title = task.title,
                        note = task.note,
                        done = task.done,
                        startAt = task.startAt,
                        dueAt = task.dueAt,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                existing.add(task.title)
                count++
            }
        }
        return count
    }

    companion object {
        private val taskRegex = Regex("""^\s*[-*+]\s+\[([ xX])\]\s+(.+)$""")
        private val startRegex = Regex("""\s*<!--\s*xstar-start:(\d+)\s*-->\s*""")
        private val dueRegex = Regex("""\s*<!--\s*xstar-due:(\d+)\s*-->\s*$""")

        data class ParsedTask(
            val title: String,
            val done: Boolean,
            val startAt: Long?,
            val dueAt: Long?,
            val note: String,
        )

        fun parseTasks(text: String): List<ParsedTask> {
            val out = mutableListOf<ParsedTask>()
            var current: ParsedTask? = null
            fun flush() {
                current?.let(out::add)
                current = null
            }
            text.lines().forEach { line ->
                val match = taskRegex.matchEntire(line)
                if (match != null) {
                    flush()
                    val rawTitle = match.groupValues[2].trim()
                    val startMatch = startRegex.find(rawTitle)
                    val dueMatch = dueRegex.find(rawTitle)
                    val cleanTitle = dueRegex.replace(startRegex.replace(rawTitle, ""), "").trim()
                    current = ParsedTask(
                        title = cleanTitle,
                        done = match.groupValues[1] != " ",
                        startAt = startMatch?.groupValues?.get(1)?.toLongOrNull(),
                        dueAt = dueMatch?.groupValues?.get(1)?.toLongOrNull(),
                        note = "",
                    )
                } else if (current != null && line.isNotBlank() && line.firstOrNull()?.isWhitespace() == true) {
                    current = current?.copy(
                        note = listOf(current?.note.orEmpty(), line.trim())
                            .filter { it.isNotBlank() }
                            .joinToString("\n"),
                    )
                }
            }
            flush()
            return out.filter { it.title.isNotBlank() }
        }

        val todoOrder: Comparator<TodoNodeEntity> = Comparator { a, b ->
            val done = a.done.compareTo(b.done)
            if (done != 0) return@Comparator done
            val aDue = a.startAt ?: a.dueAt ?: Long.MAX_VALUE
            val bDue = b.startAt ?: b.dueAt ?: Long.MAX_VALUE
            val due = aDue.compareTo(bDue)
            if (due != 0) due else b.updatedAt.compareTo(a.updatedAt)
        }

        fun timestamp(): String =
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        fun defaultFileName(): String =
            SimpleDateFormat("'todos-'yyyyMMdd-HHmm'.md'", Locale.getDefault()).format(Date())

        private fun sanitize(relPath: String): String =
            relPath.replace("\\", "/").replace("../", "").removePrefix("/").removePrefix("./")

        private fun joinPath(folder: String, fileName: String): String {
            val clean = folder.trim().replace("\\", "/").trim('/')
            return if (clean.isBlank()) fileName else "$clean/$fileName"
        }
    }
}
