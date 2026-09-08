package com.xstar.notebook.data.repo

import com.xstar.notebook.data.db.XstarDatabase
import com.xstar.notebook.data.db.entity.DocEntity
import com.xstar.notebook.data.db.entity.RepoEntity
import com.xstar.notebook.data.db.entity.TodoEntity
import com.xstar.notebook.data.git.GitClient
import com.xstar.notebook.data.secure.SecureTokenStore
import com.xstar.notebook.data.util.FileTypeMap
import com.xstar.notebook.data.util.TextEncoding
import com.xstar.notebook.domain.model.HostType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.eclipse.jgit.util.FileUtils
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class ResolveConflictResult(
    val remaining: List<String>,
    val pushed: Boolean,
)

class RepoRepository(
    private val db: XstarDatabase,
    private val git: GitClient,
    private val tokenStore: SecureTokenStore,
) {
    private val repoDao = db.repoDao()
    private val docDao = db.docDao()
    private val todoDao = db.todoDao()

    private val locks = ConcurrentHashMap<Long, Mutex>()

    private fun lockFor(repoId: Long): Mutex = locks.computeIfAbsent(repoId) { Mutex() }

    fun observeRepos(): Flow<List<RepoEntity>> = repoDao.observeAll()

    fun observeRecentDocs(limit: Int = 8): Flow<List<DocEntity>> =
        docDao.observeRecentMarkdown(limit)

    suspend fun reposOnce(): List<RepoEntity> = repoDao.getAllOnce()

    fun observeRepo(id: Long): Flow<RepoEntity?> = repoDao.observeById(id)

    fun observeTodos(repoId: Long): Flow<List<TodoEntity>> = todoDao.observeByRepo(repoId)

    fun observeTodosByDoc(repoId: Long, docRelPath: String): Flow<List<TodoEntity>> =
        todoDao.observeByDoc(repoId, docRelPath)

    suspend fun getRepo(id: Long): RepoEntity? = repoDao.getById(id)

    /** 新增仓库：先落库拿到 id，clone 成功后置为可用状态。 */
    suspend fun addRepo(
        displayName: String,
        remoteUrl: String,
        hostType: HostType,
        username: String,
        token: String,
        branch: String,
    ): Long = withContext(Dispatchers.IO) {
        val id = repoDao.insert(
            RepoEntity(
                displayName = displayName,
                remoteUrl = remoteUrl,
                hostType = hostType.name,
                username = username,
                defaultBranch = branch,
                localDir = "",
                createdAt = System.currentTimeMillis(),
                syncState = "syncing",
            ),
        )
        tokenStore.putToken(id, token)
        val entity = repoDao.getById(id)!!
        lockFor(id).withLock {
            try {
                git.clone(
                    GitClient.CloneRequest(
                        repoId = id,
                        remoteUrl = remoteUrl,
                        branch = branch.ifBlank { "main" },
                        hostType = hostType,
                        username = username,
                        token = token,
                    ),
                )
                repoDao.insert(entity.copy(localDir = "repos/$id", syncState = "idle", lastSyncAt = System.currentTimeMillis()))
                indexDocs(id)
            } catch (e: Exception) {
                repoDao.insert(entity.copy(syncState = "error"))
                tokenStore.removeToken(id)
                throw e
            }
        }
        id
    }

    suspend fun deleteRepo(repo: RepoEntity) = withContext(Dispatchers.IO) {
        repoDao.deleteById(repo.id)
        tokenStore.removeToken(repo.id)
        todoDao.deleteByRepo(repo.id)
        docDao.deleteByRepo(repo.id)
        runCatching { FileUtils.delete(git.localDirFor(repo.id), FileUtils.RECURSIVE or FileUtils.RETRY) }
    }

    suspend fun pull(repo: RepoEntity) = withContext(Dispatchers.IO) {
        lockFor(repo.id).withLock {
            repoDao.update(repo.copy(syncState = "syncing"))
            try {
                val token = tokenStore.getToken(repo.id)
                    ?: throw IllegalStateException("未找到该仓库的访问令牌，请在仓库设置中重新填写")
                git.pull(repo.id, HostType.from(repo.hostType), repo.username, token)
                repoDao.update(repo.copy(syncState = "idle", lastSyncAt = System.currentTimeMillis()))
                indexDocs(repo.id)
            } catch (e: Exception) {
                markSyncFailure(repo.id)
                throw e
            }
        }
    }

    suspend fun push(repo: RepoEntity) = withContext(Dispatchers.IO) {
        lockFor(repo.id).withLock {
            repoDao.update(repo.copy(syncState = "syncing"))
            try {
                val token = tokenStore.getToken(repo.id)
                    ?: throw IllegalStateException("未找到该仓库的访问令牌，请在仓库设置中重新填写")
                git.push(repo.id, HostType.from(repo.hostType), repo.username, token)
                repoDao.update(repo.copy(syncState = "idle", lastSyncAt = System.currentTimeMillis()))
            } catch (e: Exception) {
                repoDao.update(repo.copy(syncState = "dirty"))
                throw e
            }
        }
    }

    /** 重试推送：把本地已提交但未推送到远程的内容推上去。返回是否成功。 */
    suspend fun retryPush(repo: RepoEntity): Boolean = withContext(Dispatchers.IO) {
        lockFor(repo.id).withLock {
            val token = tokenStore.getToken(repo.id)
            if (token == null) {
                repoDao.update(repo.copy(syncState = "dirty"))
                return@withContext false
            }
            try {
                git.push(repo.id, HostType.from(repo.hostType), repo.username, token)
                repoDao.update(repo.copy(syncState = "idle", lastSyncAt = System.currentTimeMillis()))
                true
            } catch (e: Exception) {
                repoDao.update(repo.copy(syncState = "dirty"))
                false
            }
        }
    }

    suspend fun sync(repo: RepoEntity) = withContext(Dispatchers.IO) {
        lockFor(repo.id).withLock {
            repoDao.update(repo.copy(syncState = "syncing"))
            try {
                val token = tokenStore.getToken(repo.id)
                    ?: throw IllegalStateException("未找到该仓库的访问令牌，请在仓库设置中重新填写")
                git.pull(repo.id, HostType.from(repo.hostType), repo.username, token)
                git.push(repo.id, HostType.from(repo.hostType), repo.username, token)
                repoDao.update(repo.copy(syncState = "idle", lastSyncAt = System.currentTimeMillis()))
                indexDocs(repo.id)
            } catch (e: Exception) {
                markSyncFailure(repo.id)
                throw e
            }
        }
    }

    private suspend fun markSyncFailure(repoId: Long) {
        val fresh = repoDao.getById(repoId) ?: return
        val unmerged = git.listUnmerged(repoId)
        repoDao.update(fresh.copy(syncState = if (unmerged.isNotEmpty()) "conflict" else "error"))
    }

    /** 冲突文件列表（当前处于 merge 冲突状态的文件）。 */
    suspend fun listConflicts(repoId: Long): List<String> = git.listUnmerged(repoId)

    /** 解决一个冲突文件。keepOurs=true 保留本地版本，false 保留远程版本。返回剩余冲突。 */
    suspend fun resolveConflict(repo: RepoEntity, relPath: String, keepOurs: Boolean): ResolveConflictResult =
        withContext(Dispatchers.IO) {
            lockFor(repo.id).withLock {
                git.checkoutConflictStage(repo.id, relPath, keepOurs)
                git.stagePath(repo.id, relPath)
                val remaining = git.listUnmerged(repo.id)
                if (remaining.isNotEmpty()) {
                    repoDao.update(repo.copy(syncState = "conflict"))
                    ResolveConflictResult(remaining, false)
                } else {
                    // 冲突全部解决：生成 merge 提交并尝试推送
                    val pushed = commitAllAndPush(repo, "merge: 解决冲突")
                    indexDocs(repo.id)
                    ResolveConflictResult(emptyList(), pushed)
                }
            }
        }

    fun listFiles(repoId: Long, relDir: String): List<File> {
        val base = git.localDirFor(repoId)
        val dir = if (relDir.isBlank()) base else File(base, relDir.replace("..", ""))
        return git.list(dir)
    }

    suspend fun readMarkdown(repoId: Long, relPath: String): String {
        val text = git.readFileText(repoId, relPath)
        indexTodos(repoId, relPath, text)
        upsertDocRow(repoId, relPath)
        return text
    }

    /** 按指定编码读取（charsetName 为 null 时自动探测）。 */
    suspend fun readMarkdownEncoded(repoId: Long, relPath: String, charsetName: String?): String {
        val text = git.readFileTextEncoded(repoId, relPath, charsetName)
        indexTodos(repoId, relPath, text)
        upsertDocRow(repoId, relPath)
        return text
    }

    data class ParsedTodo(val lineIndex: Int, val title: String, val checked: Boolean, val position: Int)

    private val todoRegex = Regex("""^\s*[-*+]\s+\[([ xX])\]\s+(.+)$""")

    fun parseTodos(text: String): List<ParsedTodo> {
        return text.lines()
            .mapIndexedNotNull { index, line ->
                val m = todoRegex.matchEntire(line) ?: return@mapIndexedNotNull null
                ParsedTodo(
                    lineIndex = index,
                    title = m.groupValues[2],
                    checked = m.groupValues[1] == "x" || m.groupValues[1] == "X",
                    position = index,
                )
            }
    }

    /** 保存文档改动：写文件 → 重建 TODO → 本地提交；有令牌则推送到远程。 */
    suspend fun updateMarkdown(
        repo: RepoEntity,
        relPath: String,
        content: String,
        pushAfter: Boolean = true,
        extraFilePaths: List<String> = emptyList(),
    ): Boolean = withContext(Dispatchers.IO) {
        lockFor(repo.id).withLock {
            git.writeFileText(repo.id, relPath, content)
            indexTodos(repo.id, relPath, content)
            upsertDocRow(repo.id, relPath)
            git.commit(repo.id, "edit: $relPath", listOf(relPath) + extraFilePaths)
            if (!pushAfter) {
                repoDao.update(repo.copy(syncState = "dirty"))
                return@withContext false
            }
            val token = tokenStore.getToken(repo.id)
            if (token == null) {
                repoDao.update(repo.copy(syncState = "dirty"))
                return@withContext false
            }
            try {
                git.push(repo.id, HostType.from(repo.hostType), repo.username, token)
                repoDao.update(repo.copy(syncState = "idle", lastSyncAt = System.currentTimeMillis()))
                true
            } catch (e: Exception) {
                repoDao.update(repo.copy(syncState = "dirty"))
                false
            }
        }
    }

    private suspend fun commitAllAndPush(repo: RepoEntity, message: String): Boolean {
        git.stageAll(repo.id)
        git.commit(repo.id, message, null)
        val token = tokenStore.getToken(repo.id)
        if (token == null) {
            repoDao.update(repo.copy(syncState = "dirty"))
            return false
        }
        return try {
            git.push(repo.id, HostType.from(repo.hostType), repo.username, token)
            repoDao.update(repo.copy(syncState = "idle", lastSyncAt = System.currentTimeMillis()))
            true
        } catch (e: Exception) {
            repoDao.update(repo.copy(syncState = "dirty"))
            false
        }
    }

    /** 重命名仓库内文件或目录，并提交改动（有令牌则推送）。 */
    suspend fun renameNode(repo: RepoEntity, relPath: String, newName: String): Boolean =
        withContext(Dispatchers.IO) {
            if (newName.isBlank() || newName.contains('/') || newName.contains('\\')) {
                throw IllegalArgumentException("名称不能为空或包含 / 或 \\")
            }
            lockFor(repo.id).withLock {
                val from = File(git.localDirFor(repo.id), relPath)
                if (!from.exists()) throw IllegalArgumentException("文件不存在")
                val to = File(from.parentFile ?: git.localDirFor(repo.id), newName)
                if (to.exists()) throw IllegalArgumentException("目标已存在同名文件")
                val isDir = from.isDirectory
                if (!from.renameTo(to)) throw IllegalStateException("重命名失败")
                val newRel = relOfParent(relPath, newName)
                val pushed = commitAllAndPush(repo, "rename: $relPath -> $newRel")
                if (isDir) {
                    indexDocs(repo.id)
                } else {
                    docDao.deletePaths(repo.id, listOf(relPath))
                    todoDao.deleteByDoc(repo.id, relPath)
                    val text = git.readFileText(repo.id, newRel)
                    indexTodos(repo.id, newRel, text)
                    upsertDocRow(repo.id, newRel)
                }
                pushed
            }
        }

    /** 删除仓库内文件或目录（含子内容），并提交改动（有令牌则推送）。 */
    suspend fun deleteNode(repo: RepoEntity, relPath: String): Boolean =
        withContext(Dispatchers.IO) {
            lockFor(repo.id).withLock {
                val node = File(git.localDirFor(repo.id), relPath)
                if (!node.exists()) throw IllegalArgumentException("文件不存在")
                val isDir = node.isDirectory
                FileUtils.delete(node, FileUtils.RECURSIVE or FileUtils.RETRY)
                val pushed = commitAllAndPush(repo, "delete: $relPath")
                if (isDir) {
                    indexDocs(repo.id)
                } else {
                    docDao.deletePaths(repo.id, listOf(relPath))
                    todoDao.deleteByDoc(repo.id, relPath)
                }
                pushed
            }
        }

    private fun relOfParent(relPath: String, newName: String): String {
        val idx = relPath.lastIndexOf('/')
        return if (idx < 0) newName else relPath.substring(0, idx + 1) + newName
    }

    /** 在指定目录新建 Markdown 文件（自动建目录），返回相对路径；提交改动（有令牌则推送）。 */
    suspend fun createMarkdownFile(repo: RepoEntity, relFolder: String, fileName: String): String =
        withContext(Dispatchers.IO) {
            if (fileName.isBlank() || fileName.contains('/') || fileName.contains('\\')) {
                throw IllegalArgumentException("文件名不能为空或包含 / 或 \\")
            }
            val folder = relFolder.trim().replace("\\", "/").replace("../", "").trim('/')
            val rel = if (folder.isBlank()) fileName else "$folder/$fileName"
            lockFor(repo.id).withLock {
                val target = File(git.localDirFor(repo.id), rel)
                if (target.exists()) throw IllegalArgumentException("该文件已存在")
                val heading = "# " + fileName.removeSuffix(".md").trim().ifBlank { "Untitled" }
                git.writeFileText(repo.id, rel, "$heading\n\n")
                val pushed = commitAllAndPush(repo, "create: $rel")
                indexTodos(repo.id, rel, "$heading\n\n")
                upsertDocRow(repo.id, rel)
                pushed
            }
            rel
        }

    /**
     * 新建文件或目录（文件无扩展名时默认 .md），并提交改动。
     * 返回新节点的仓库内相对路径；目录会放一个 .gitkeep 以便被 git 跟踪。
     */
    suspend fun createFileOrFolder(
        repo: RepoEntity,
        relFolder: String,
        rawName: String,
        isDirectory: Boolean,
    ): String = withContext(Dispatchers.IO) {
        if (rawName.isBlank() || rawName.contains('/') || rawName.contains('\\')) {
            throw IllegalArgumentException("名称不能为空或包含 / 或 \\")
        }
        val folder = relFolder.trim().replace("\\", "/").replace("../", "").trim('/')
        var name = rawName.trim()
        if (!isDirectory && !name.contains('.')) name += ".md"
        val rel = if (folder.isBlank()) name else "$folder/$name"
        lockFor(repo.id).withLock {
            if (isDirectory) {
                val dir = File(git.localDirFor(repo.id), rel)
                if (dir.exists()) throw IllegalArgumentException("已存在同名文件或文件夹")
                FileUtils.mkdirs(dir, true)
                File(dir, ".gitkeep").writeText("", Charsets.UTF_8)
                commitAllAndPush(repo, "create folder: $rel/")
                indexDocs(repo.id)
            } else {
                val target = File(git.localDirFor(repo.id), rel)
                if (target.exists()) throw IllegalArgumentException("该文件已存在")
                val isMd = name.lowercase().endsWith(".md") || name.lowercase().endsWith(".markdown")
                val content = if (isMd) "# ${name.removeSuffix(".md").removeSuffix(".markdown").trim().ifBlank { "Untitled" }}\n\n" else ""
                git.writeFileText(repo.id, rel, content)
                commitAllAndPush(repo, "create: $rel")
                indexTodos(repo.id, rel, content)
                upsertDocRow(repo.id, rel)
            }
            rel
        }
    }

    /** 勾选/取消勾选某个 TODO：改写原 md 行 → 提交 → 推送。 */
    suspend fun toggleTodo(repo: RepoEntity, todo: TodoEntity, newChecked: Boolean, pushAfter: Boolean = true) =
        withContext(Dispatchers.IO) {
            lockFor(repo.id).withLock {
                val relPath = todo.docRelPath
                val text = git.readFileText(repo.id, relPath)
                val lines = text.split("\n").toMutableList()
                if (todo.lineIndex in lines.indices) {
                    val check = if (newChecked) "x" else " "
                    var line = lines[todo.lineIndex]
                    line = line.replaceFirst(Regex("""(\[)[ xX](\])"""), "[$check]")
                    lines[todo.lineIndex] = line
                }
                val updated = lines.joinToString("\n")
                git.writeFileText(repo.id, relPath, updated)
                indexTodos(repo.id, relPath, updated)
                upsertDocRow(repo.id, relPath)
                git.commit(repo.id, "todo: ${if (newChecked) "✔" else "☐"} $relPath", listOf(relPath))
                if (pushAfter) {
                    val token = tokenStore.getToken(repo.id)
                    if (token != null) {
                        try {
                            git.push(repo.id, HostType.from(repo.hostType), repo.username, token)
                            repoDao.update(repo.copy(syncState = "idle", lastSyncAt = System.currentTimeMillis()))
                        } catch (e: Exception) {
                            repoDao.update(repo.copy(syncState = "dirty"))
                        }
                    } else {
                        repoDao.update(repo.copy(syncState = "dirty"))
                    }
                }
            }
        }

    /** 把外部图片字节写入仓库目录（不提交），返回仓库内相对路径。 */
    suspend fun importImageBytes(repo: RepoEntity, folder: String, bytes: ByteArray, ext: String): String =
        withContext(Dispatchers.IO) {
            val cleanFolder = folder.trim().replace("\\", "/").replace("../", "").trim('/')
            var name = "image-${System.currentTimeMillis()}.${sanitizeExt(ext)}"
            var rel = if (cleanFolder.isBlank()) name else "$cleanFolder/$name"
            var counter = 1
            while (File(git.localDirFor(repo.id), rel).exists()) {
                name = "image-${System.currentTimeMillis()}-${counter++}.${sanitizeExt(ext)}"
                rel = if (cleanFolder.isBlank()) name else "$cleanFolder/$name"
            }
            git.writeFileBytes(repo.id, rel, bytes)
            upsertDocRow(repo.id, rel)
            rel
        }

    /** 删除尚未提交到 git 的本地文件（例如放弃编辑时清理插入的图片），不做提交。 */
    suspend fun removeLocalFile(repoId: Long, relPath: String) = withContext(Dispatchers.IO) {
        git.deleteLocal(repoId, relPath)
        docDao.deletePaths(repoId, listOf(relPath))
    }

    private fun sanitizeExt(ext: String): String {
        val clean = ext.lowercase().filter { it.isLetterOrDigit() }
        return clean.ifEmpty { "png" }
    }

    // ------------------------------------------------------------------
    // 搜索
    // ------------------------------------------------------------------

    data class SearchHit(
        val repoId: Long,
        val repoName: String,
        val relPath: String,
        val type: String,
        val line: Int = -1,
        val snippet: String = "",
        val fileNameHit: Boolean = false,
    )

    /** 跨仓库搜索：文件名命中 + 文本内容命中（md/txt/code）。 */
    suspend fun searchGlobal(repos: List<RepoEntity>, query: String): List<SearchHit> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.isEmpty()) return@withContext emptyList()
            val lowerQ = q.lowercase()
            val results = ArrayList<SearchHit>()
            var total = 0
            for (repo in repos) {
                if (total >= 200) break
                val root = git.localDirFor(repo.id)
                if (!root.exists()) continue
                val files = ArrayList<Pair<File, String>>()
                collectFiles(root, "", files)
                for ((file, rel) in files) {
                    if (total >= 200) break
                    val name = file.name
                    val nameHit = name.lowercase().contains(lowerQ)
                    var addedContent = false
                    if (FileTypeMap.isTextLike(name) && file.length() <= 2L * 1024 * 1024) {
                        val content = runCatching { TextEncoding.decode(file.readBytes()) }.getOrNull()
                        if (content != null) {
                            var matched = 0
                            content.lines().forEachIndexed { idx, line ->
                                if (total >= 200 || matched >= 5) return@forEachIndexed
                                if (line.lowercase().contains(lowerQ)) {
                                    results.add(
                                        SearchHit(
                                            repoId = repo.id,
                                            repoName = repo.displayName,
                                            relPath = rel,
                                            type = FileTypeMap.type(name),
                                            line = idx + 1,
                                            snippet = line.trim().take(180),
                                        ),
                                    )
                                    total++
                                    matched++
                                    addedContent = true
                                }
                            }
                        }
                    }
                    if (nameHit && !addedContent) {
                        val type = FileTypeMap.type(name)
                        if (type in setOf("md", "text", "code")) {
                            results.add(
                                SearchHit(
                                    repoId = repo.id,
                                    repoName = repo.displayName,
                                    relPath = rel,
                                    type = type,
                                    fileNameHit = true,
                                ),
                            )
                            total++
                        }
                    }
                }
            }
            results.sortedWith(
                compareByDescending<SearchHit> { it.snippet.isNotEmpty() }.thenBy { it.repoName }.thenBy { it.relPath },
            )
        }

    private fun collectFiles(dir: File, prefix: String, out: MutableList<Pair<File, String>>) {
        val children = dir.listFiles() ?: return
        for (f in children) {
            val rel = if (prefix.isEmpty()) f.name else "$prefix/${f.name}"
            if (f.isDirectory) {
                if (f.name == ".git" || f.name == ".idea") continue
                collectFiles(f, rel, out)
            } else {
                out.add(f to rel)
            }
        }
    }

    // ------------------------------------------------------------------
    // 双链 / 反向链接
    // ------------------------------------------------------------------

    /**
     * 找出仓库内引用了 [relPath] 文档的其它 Markdown 文件。
     * 识别两种语法：`[[目标]]` 以及 `[文字](相对路径)`。
     */
    suspend fun findBacklinks(repoId: Long, targetRelPath: String): List<String> =
        withContext(Dispatchers.IO) {
            val root = git.localDirFor(repoId)
            if (!root.exists()) return@withContext emptyList()
            val targetFile = root.resolve(targetRelPath)
            val targetNameLower = targetFile.nameWithoutExtension.lowercase()
            val targetRelLower = targetRelPath.lowercase()
            val out = ArrayList<String>()
            val files = ArrayList<Pair<File, String>>()
            collectFiles(root, "", files)
            for ((file, rel) in files) {
                if (rel == targetRelPath) continue
                if (!FileTypeMap.isTextLike(file.name) || file.length() > 2L * 1024 * 1024) continue
                val content = runCatching { TextEncoding.decode(file.readBytes()) }.getOrNull() ?: continue
                val baseDir = if (rel.contains('/')) rel.substringBeforeLast('/') else ""
                var linked = false
                val wikiRe = Regex("""\[\[([^\]|]+?)(?:\|[^\]]*)?\]\]""")
                for (m in wikiRe.findAll(content)) {
                    if (linked) break
                    val target = m.groupValues[1].trim().removePrefix("./").removePrefix("/")
                    val targetLower = target.lowercase()
                    val targetNoExt = target.substringAfterLast('/').removeSuffix(".md").removeSuffix(".markdown")
                    if (targetNoExt.lowercase() == targetNameLower) { linked = true; break }
                    if (resolveRel(baseDir, targetLower) == targetRelLower) { linked = true; break }
                }
                if (!linked) {
                    val linkRe = Regex("""(?<!!)\[[^\]]*]\(([^)\s]+?)(?:\s+["'][^"']*["'])?\)""")
                    for (m in linkRe.findAll(content)) {
                        val target = m.groupValues[1]
                        if (target.startsWith("#") || target.startsWith("http://") ||
                            target.startsWith("https://") || target.startsWith("mailto:") ||
                            target.startsWith("data:")
                        ) {
                            continue
                        }
                        val targetLower = target.lowercase().substringBefore('#').trim()
                        if (resolveRel(baseDir, targetLower) == targetRelLower) { linked = true; break }
                    }
                }
                if (linked) out.add(rel)
            }
            out.sorted()
        }

    private fun resolveRel(baseDir: String, target: String): String {
        val base = if (baseDir.isEmpty()) listOf() else baseDir.split('/')
        val parts = ArrayList<String>()
        target.split('/').forEach { seg ->
            when (seg) {
                "", "." -> {}
                ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.size - 1)
                else -> parts.add(seg)
            }
        }
        return (base + parts).joinToString("/").lowercase()
    }

    // ------------------------------------------------------------------
    // 文档索引（docs 表）与 TODO 重建
    // ------------------------------------------------------------------

    /** 全量刷新某仓库的 docs 元数据索引，并重建全部 md 的 TODO 索引。 */
    private suspend fun indexDocs(repoId: Long) {
        val root = git.localDirFor(repoId)
        if (!root.exists()) return
        val docs = ArrayList<DocEntity>()
        val allTodos = ArrayList<TodoEntity>()
        val files = ArrayList<Pair<File, String>>()
        collectFiles(root, "", files)
        for ((file, rel) in files) {
            val type = FileTypeMap.type(file.name)
            docs.add(
                DocEntity(
                    repoId = repoId,
                    relPath = rel,
                    fileType = type,
                    size = file.length(),
                    modifiedAt = file.lastModified(),
                ),
            )
            if (type == "md" && file.length() <= 1024L * 1024) {
                val text = runCatching { TextEncoding.decode(file.readBytes()) }.getOrNull() ?: continue
                val parsed = parseTodos(text)
                parsed.forEach { p ->
                    allTodos.add(
                        TodoEntity(
                            repoId = repoId,
                            docRelPath = rel,
                            lineIndex = p.lineIndex,
                            title = p.title,
                            checked = p.checked,
                            position = p.position,
                        ),
                    )
                }
            }
        }
        docDao.deleteByRepo(repoId)
        todoDao.deleteByRepo(repoId)
        docDao.upsertAll(docs)
        todoDao.upsertAll(allTodos)
    }

    /** pull 后若文件内容校验不一致则重建该文档的 TODO。全量索引已在 indexDocs 内完成。 */
    private suspend fun reindexTodosIfNeeded(repo: RepoEntity) {
        // 见 indexDocs：pull/sync 已整仓重建 todos
    }

    /** 单文档 TODO 重建（写文件后调用）。 */
    private suspend fun indexTodos(repoId: Long, relPath: String, text: String) {
        val parsed = parseTodos(text)
        todoDao.deleteByDoc(repoId, relPath)
        todoDao.upsertAll(
            parsed.map {
                TodoEntity(
                    repoId = repoId,
                    docRelPath = relPath,
                    lineIndex = it.lineIndex,
                    title = it.title,
                    checked = it.checked,
                    position = it.position,
                )
            },
        )
    }

    /** 刷新单个文档的 docs 元数据行。 */
    private suspend fun upsertDocRow(repoId: Long, relPath: String) {
        val file = git.absolutePath(repoId, relPath)
        if (!file.exists()) return
        docDao.upsert(
            DocEntity(
                repoId = repoId,
                relPath = relPath,
                fileType = FileTypeMap.type(file.name),
                size = file.length(),
                modifiedAt = file.lastModified(),
            ),
        )
    }
}
