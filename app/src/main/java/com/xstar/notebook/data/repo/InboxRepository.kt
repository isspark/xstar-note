package com.xstar.notebook.data.repo

import com.xstar.notebook.data.db.XstarDatabase
import com.xstar.notebook.data.db.entity.InboxItemEntity
import com.xstar.notebook.data.git.GitClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File

class InboxRepository(
    private val db: XstarDatabase,
    private val todoHubRepository: TodoHubRepository,
    private val git: GitClient,
) {
    private val dao = db.inboxDao()

    fun observeAll(): Flow<List<InboxItemEntity>> = dao.observeAll()

    fun observePendingCount(): Flow<Int> = dao.observePendingCount()

    suspend fun get(id: Long): InboxItemEntity? = dao.getById(id)

    suspend fun add(
        type: String,
        title: String,
        content: String,
        url: String? = null,
        source: String? = null,
    ): Long {
        require(type in setOf(InboxItemEntity.TYPE_TEXT, InboxItemEntity.TYPE_LINK, InboxItemEntity.TYPE_TODO))
        require(title.isNotBlank() || content.isNotBlank() || !url.isNullOrBlank()) { "内容不能为空" }
        val now = System.currentTimeMillis()
        return dao.insert(
            InboxItemEntity(
                type = type,
                title = title.trim(),
                content = content.trim(),
                url = url?.trim()?.ifBlank { null },
                source = source?.trim()?.ifBlank { null },
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun update(item: InboxItemEntity, title: String, content: String, url: String?) {
        require(title.isNotBlank() || content.isNotBlank() || !url.isNullOrBlank()) { "内容不能为空" }
        dao.update(
            item.copy(
                title = title.trim(),
                content = content.trim(),
                url = url?.trim()?.ifBlank { null },
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun archive(item: InboxItemEntity) {
        dao.update(item.copy(status = InboxItemEntity.STATUS_ARCHIVED, updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(item: InboxItemEntity) = dao.deleteById(item.id)

    suspend fun convertToTodo(item: InboxItemEntity): Long {
        if (item.status == InboxItemEntity.STATUS_CONVERTED && item.targetType == "todo" && item.targetId != null) {
            return item.targetId
        }
        val title = item.title.ifBlank { item.content.lineSequence().firstOrNull().orEmpty() }.trim()
        val note = listOf(item.content, item.url?.let { "来源：$it" }.orEmpty())
            .filter { it.isNotBlank() && it != title }
            .joinToString("\n")
        val todoId = todoHubRepository.addNode(title.ifBlank { "未命名任务" }, note)
        dao.update(
            item.copy(
                status = InboxItemEntity.STATUS_CONVERTED,
                targetType = "todo",
                targetId = todoId,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        return todoId
    }

    suspend fun convertToMarkdown(item: InboxItemEntity, repoId: Long, folder: String, fileName: String): String {
        if (item.status == InboxItemEntity.STATUS_CONVERTED && item.targetType == "markdown") {
            return item.targetPath.orEmpty()
        }
        val cleanName = fileName.trim().let {
            if (it.endsWith(".md", true) || it.endsWith(".markdown", true)) it else "$it.md"
        }
        val cleanFolder = folder.trim().replace("\\", "/").replace("../", "").trim('/')
        val relPath = if (cleanFolder.isBlank()) cleanName else "$cleanFolder/$cleanName"
        val content = buildString {
            appendLine("# ${item.title.ifBlank { cleanName.removeSuffix(".md").removeSuffix(".markdown") }}")
            appendLine()
            if (item.content.isNotBlank()) appendLine(item.content)
            if (!item.url.isNullOrBlank()) {
                if (item.content.isNotBlank()) appendLine()
                appendLine("来源：${item.url}")
            }
        }
        withContext(Dispatchers.IO) {
            val target = File(git.localDirFor(repoId), relPath)
            require(!target.exists()) { "目标文件已存在" }
            target.parentFile?.mkdirs()
            target.writeText(content, Charsets.UTF_8)
        }
        git.commit(repoId, "inbox: save $cleanName", listOf(relPath))
        dao.update(
            item.copy(
                status = InboxItemEntity.STATUS_CONVERTED,
                targetType = "markdown",
                targetId = null,
                targetPath = relPath,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        return relPath
    }
}
