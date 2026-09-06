package com.xstar.notebook.data.git

import android.content.Context
import com.xstar.notebook.data.util.TextEncoding
import com.xstar.notebook.domain.model.HostType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.util.FileUtils
import java.io.File
import java.io.IOException

/**
 * JGit 封装：clone / pull / push / commit。所有操作在 Dispatchers.IO 执行，
 * 调用方对同一仓库需持有一把 Mutex 串行化（见 RepoRepository）。
 */
class GitClient(context: Context) {

    private val reposRoot = File(context.filesDir, "repos")

    fun localDirFor(repoId: Long): File = File(reposRoot, repoId.toString())

    data class CloneRequest(
        val repoId: Long,
        val remoteUrl: String,
        val branch: String,
        val hostType: HostType,
        val username: String,
        val token: String,
    )

    suspend fun clone(request: CloneRequest) = withContext(Dispatchers.IO) {
        val dir = localDirFor(request.repoId)
        if (File(dir, Constants.DOT_GIT).exists()) return@withContext
        FileUtils.mkdirs(dir, true)
        val cp = CredentialFactory.create(request.hostType, request.username, request.token)
        try {
            Git.cloneRepository()
                .setURI(request.remoteUrl)
                .setDirectory(dir)
                .setBranch(request.branch)
                .setCredentialsProvider(cp)
                .setTimeout(30)
                .call()
                .use { git -> ensureIdentity(git.repository) }
        } catch (e: GitAPIException) {
            cleanFailedClone(dir)
            throw GitOperationException(e.message ?: "clone 失败", e)
        }
    }

    suspend fun pull(repoId: Long, hostType: HostType, username: String, token: String) =
        withContext(Dispatchers.IO) {
            open(repoId).use { git ->
                val cp = CredentialFactory.create(hostType, username, token)
                git.pull()
                    // 采用 merge 模式而非 rebase：发生冲突时能保留可解析的冲突状态，
                    // 让用户可以逐文件选择“保留本地”或“保留远程”。
                    .setRebase(false)
                    .setCredentialsProvider(cp)
                    .setTimeout(30)
                    .call()
            }
        }

    suspend fun push(repoId: Long, hostType: HostType, username: String, token: String) =
        withContext(Dispatchers.IO) {
            open(repoId).use { git ->
                val cp = CredentialFactory.create(hostType, username, token)
                git.push()
                    .setCredentialsProvider(cp)
                    .setTimeout(30)
                    .call()
            }
        }

    /** 将工作区所有改动加入暂存区（含删除与重命名）。 */
    suspend fun stageAll(repoId: Long) = withContext(Dispatchers.IO) {
        open(repoId).use { git ->
            git.add().addFilepattern(".").call()
            git.add().addFilepattern(".").setUpdate(true).call()
        }
    }

    /** 提交一个或多个相对路径文件的改动，并将该文件加入索引。 */
    suspend fun commit(repoId: Long, message: String, filePaths: List<String>? = null) =
        withContext(Dispatchers.IO) {
            open(repoId).use { git ->
                val add = git.add()
                if (filePaths.isNullOrEmpty()) add.addFilepattern(".") else filePaths.forEach(add::addFilepattern)
                add.call()
                val repo = git.repository
                ensureIdentity(repo)
                val who = PersonIdent(
                    repo.config.getString("user", null, "name") ?: "xstar-notebook",
                    repo.config.getString("user", null, "email") ?: "notebook@xstar.local",
                )
                git.commit()
                    .setAuthor(who)
                    .setCommitter(who)
                    .setMessage(message)
                    .setSign(false)
                    .call()
            }
        }

    suspend fun readFileText(repoId: Long, relPath: String): String = withContext(Dispatchers.IO) {
        val file = File(localDirFor(repoId), relPath)
        TextEncoding.decode(file.readBytes())
    }

    /** 按指定编码读取文本；charsetName 为 null 时自动探测。 */
    suspend fun readFileTextEncoded(repoId: Long, relPath: String, charsetName: String?): String =
        withContext(Dispatchers.IO) {
            val bytes = File(localDirFor(repoId), relPath).readBytes()
            if (charsetName == null) {
                TextEncoding.decode(bytes)
            } else {
                String(bytes, java.nio.charset.Charset.forName(charsetName))
            }
        }

    suspend fun writeFileText(repoId: Long, relPath: String, content: String) =
        withContext(Dispatchers.IO) {
            val file = File(localDirFor(repoId), relPath)
            val charset = if (file.exists()) {
                TextEncoding.charsetForWrite(file.readBytes())
            } else {
                Charsets.UTF_8
            }
            try {
                FileUtils.mkdirs(file.parentFile ?: localDirFor(repoId), true)
                file.writeText(content, charset)
            } catch (e: Exception) {
                android.util.Log.e("GitClient", "writeFileText fail repo=$repoId rel=$relPath abs=${file.absolutePath}", e)
                throw e
            }
        }

    /** 写入二进制文件（如图片），自动建目录。 */
    suspend fun writeFileBytes(repoId: Long, relPath: String, bytes: ByteArray) =
        withContext(Dispatchers.IO) {
            val file = File(localDirFor(repoId), relPath)
            try {
                FileUtils.mkdirs(file.parentFile ?: localDirFor(repoId), true)
                file.writeBytes(bytes)
            } catch (e: Exception) {
                android.util.Log.e("GitClient", "writeFileBytes fail repo=$repoId rel=$relPath abs=${file.absolutePath}", e)
                throw e
            }
        }

    /** 读取一个仓库内相对路径文件。 */
    suspend fun readFileBytes(repoId: Long, relPath: String): ByteArray =
        withContext(Dispatchers.IO) {
            File(localDirFor(repoId), relPath).readBytes()
        }

    /** 不提交地删除本地文件/目录（用于放弃尚未保存的草稿/资产）。 */
    suspend fun deleteLocal(repoId: Long, relPath: String) = withContext(Dispatchers.IO) {
        val file = File(localDirFor(repoId), relPath)
        if (file.exists()) {
            FileUtils.delete(file, FileUtils.RECURSIVE or FileUtils.RETRY)
        }
    }

    /** 当前处于冲突状态的文件相对路径列表（含目录层级）。 */
    suspend fun listUnmerged(repoId: Long): List<String> = withContext(Dispatchers.IO) {
        runCatching {
            open(repoId).use { git ->
                git.status().call().conflicting.sorted()
            }
        }.getOrDefault(emptyList())
    }

    /** 冲突时按阶段检出文件内容。keepOurs=true 保留本地(HEAD)版本，否则保留远程(incoming)版本。 */
    suspend fun checkoutConflictStage(repoId: Long, relPath: String, keepOurs: Boolean) =
        withContext(Dispatchers.IO) {
            open(repoId).use { git ->
                git.checkout()
                    .setStage(if (keepOurs) org.eclipse.jgit.api.CheckoutCommand.Stage.OURS else org.eclipse.jgit.api.CheckoutCommand.Stage.THEIRS)
                    .addPath(relPath)
                    .call()
            }
        }

    /** 将冲突文件加入暂存区，标记为已解决。 */
    suspend fun stagePath(repoId: Long, relPath: String) = withContext(Dispatchers.IO) {
        open(repoId).use { git ->
            git.add().addFilepattern(relPath).call()
        }
    }

    fun absolutePath(repoId: Long, relPath: String): File = File(localDirFor(repoId), relPath)

    fun list(repoDir: File): List<File> =
        repoDir.listFiles()
            ?.filter { it.name != Constants.DOT_GIT && it.name != ".idea" }
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()

    private fun open(repoId: Long): Git {
        val builder = FileRepositoryBuilder()
        val repo = builder.setGitDir(File(localDirFor(repoId), Constants.DOT_GIT)).build()
        return Git(repo)
    }

    private fun ensureIdentity(repo: Repository) {
        val cfg = repo.config
        if (cfg.getString("user", null, "name").isNullOrBlank()) {
            cfg.setString("user", null, "name", "xstar-notebook")
            cfg.save()
        }
        if (cfg.getString("user", null, "email").isNullOrBlank()) {
            cfg.setString("user", null, "email", "notebook@xstar.local")
            cfg.save()
        }
    }

    private fun cleanFailedClone(dir: File) {
        try {
            FileUtils.delete(dir, FileUtils.RECURSIVE or FileUtils.RETRY)
        } catch (_: IOException) {
        }
    }
}

class GitOperationException(message: String, cause: Throwable? = null) : Exception(message, cause)
