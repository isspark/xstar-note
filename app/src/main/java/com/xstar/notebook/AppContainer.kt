package com.xstar.notebook

import android.content.Context
import com.xstar.notebook.data.db.XstarDatabase
import com.xstar.notebook.data.git.GitClient
import com.xstar.notebook.data.repo.RepoRepository
import com.xstar.notebook.data.repo.InboxRepository
import com.xstar.notebook.data.repo.TodoHubRepository
import com.xstar.notebook.data.secure.SecureTokenStore
import com.xstar.notebook.data.settings.AppSettings

/**
 * 简易手动依赖容器（MVP 不引入 DI 框架）。
 */
class AppContainer(context: Context) {
    val database: XstarDatabase = XstarDatabase.getInstance(context)
    val tokenStore: SecureTokenStore = SecureTokenStore(context)
    val gitClient: GitClient = GitClient(context)
    val repoRepository: RepoRepository = RepoRepository(database, gitClient, tokenStore)
    val todoHubRepository: TodoHubRepository = TodoHubRepository(database, gitClient)
    val inboxRepository: InboxRepository = InboxRepository(database, todoHubRepository, gitClient)
    val settings: AppSettings = AppSettings(context)
    val taskNotificationController = TaskNotificationController(context, settings, todoHubRepository)
}
