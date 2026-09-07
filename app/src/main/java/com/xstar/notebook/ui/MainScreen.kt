package com.xstar.notebook.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xstar.notebook.data.db.entity.RepoEntity
import com.xstar.notebook.ui.addRepo.AddRepoScreen
import com.xstar.notebook.ui.browse.BrowseScreen
import com.xstar.notebook.ui.components.BrandTitleLine
import com.xstar.notebook.ui.components.FileKind
import com.xstar.notebook.ui.components.FileTypes
import com.xstar.notebook.ui.drawioView.DrawioScreen
import com.xstar.notebook.ui.home.HomePagerScreen
import com.xstar.notebook.ui.mdView.MdViewScreen
import com.xstar.notebook.ui.navigation.Routes
import com.xstar.notebook.ui.repoList.RepoListScreen
import com.xstar.notebook.ui.settings.SettingsScreen
import com.xstar.notebook.ui.theme.BrandGradient
import com.xstar.notebook.ui.todoHub.TodoClassifyScreen
import com.xstar.notebook.ui.todoHub.TodoEditorScreen
import com.xstar.notebook.ui.todoHub.TodoHubScreen
import com.xstar.notebook.ui.todoList.TodoListScreen
import com.xstar.notebook.data.settings.HomeMode
import kotlinx.coroutines.launch

@Composable
fun MainScreen() {
    val container = appContainer()
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val repos by remember { container.repoRepository.observeRepos() }
        .collectAsState(initial = emptyList())
    val defaultRepoId by container.settings.defaultRepoId.collectAsState()
    val homeMode by container.settings.homeMode.collectAsState()

    val defaultRepo = repos.find { it.id == defaultRepoId }
    var redirected by remember { mutableStateOf(false) }

    fun openDrawer() {
        scope.launch { drawerState.open() }
    }

    fun closeDrawer() {
        scope.launch { drawerState.close() }
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    fun openRepo(id: Long) {
        redirected = true
        if (defaultRepoId <= 0L) container.settings.setDefaultRepo(id)
        nav.navigate(Routes.browse(id)) {
            popUpTo(Routes.REPOS) { inclusive = false }
        }
    }

    fun openDocByKind(repoId: Long, relPath: String, kind: FileKind) {
        when (kind) {
            FileKind.MD -> nav.navigate(Routes.mdView(repoId, relPath))
            FileKind.TEXT, FileKind.CODE -> nav.navigate(Routes.codeView(repoId, relPath))
            FileKind.DRAWIO -> nav.navigate(Routes.drawio(repoId, relPath))
            else -> Unit
        }
    }

    fun openSearch() {
        nav.navigate(Routes.SEARCH) {
            popUpTo(Routes.REPOS) { inclusive = false }
            launchSingleTop = true
        }
    }

    fun configuredHomeRoute(): String = when (homeMode) {
        HomeMode.TODO -> Routes.TODO_HUB
        HomeMode.REPOSITORY -> defaultRepo?.let { Routes.browse(it.id) } ?: Routes.REPOS
        HomeMode.BOTH -> defaultRepo?.let { Routes.homeBoth(it.id, todoFirst = false) } ?: Routes.REPOS
        HomeMode.TODO_AND_REPOSITORY -> defaultRepo?.let {
            Routes.homeBoth(it.id, todoFirst = true)
        } ?: Routes.REPOS
    }

    fun openConfiguredHome() {
        val route = configuredHomeRoute()
        if (!nav.popBackStack(route, inclusive = false)) {
            nav.navigate(route) {
                popUpTo(Routes.REPOS) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(repos, homeMode) {
        if (redirected) return@LaunchedEffect
        when (homeMode) {
            HomeMode.TODO -> {
                redirected = true
                nav.navigate(Routes.TODO_HUB) {
                    popUpTo(Routes.REPOS) { inclusive = true }
                    launchSingleTop = true
                }
            }
            HomeMode.REPOSITORY -> repos.find { it.id == defaultRepoId }?.let { repo ->
                redirected = true
                nav.navigate(Routes.browse(repo.id)) {
                    popUpTo(Routes.REPOS) { inclusive = true }
                }
            }
            HomeMode.BOTH -> repos.find { it.id == defaultRepoId }?.let { repo ->
                redirected = true
                nav.navigate(Routes.homeBoth(repo.id, todoFirst = false)) {
                    popUpTo(Routes.REPOS) { inclusive = true }
                }
            }
            HomeMode.TODO_AND_REPOSITORY -> repos.find { it.id == defaultRepoId }?.let { repo ->
                redirected = true
                nav.navigate(Routes.homeBoth(repo.id, todoFirst = true)) {
                    popUpTo(Routes.REPOS) { inclusive = true }
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                homeSubtitle = when (homeMode) {
                    HomeMode.REPOSITORY -> defaultRepo?.displayName ?: "选择默认仓库"
                    HomeMode.TODO -> "TODO"
                    HomeMode.BOTH -> "${defaultRepo?.displayName ?: "仓库"} + TODO"
                    HomeMode.TODO_AND_REPOSITORY -> "TODO + ${defaultRepo?.displayName ?: "仓库"}"
                },
                onOpenHome = {
                    closeDrawer()
                    openConfiguredHome()
                },
                onManage = {
                    closeDrawer()
                    nav.navigate(Routes.REPOS) {
                        popUpTo(Routes.REPOS) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onTodoHub = {
                    closeDrawer()
                    nav.navigate(Routes.TODO_HUB) {
                        popUpTo(Routes.REPOS) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onSearch = {
                    closeDrawer()
                    openSearch()
                },
                onSettings = {
                    closeDrawer()
                    nav.navigate(Routes.SETTINGS) {
                        popUpTo(Routes.REPOS) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        },
    ) {
        NavHost(navController = nav, startDestination = Routes.REPOS) {
            composable(Routes.REPOS) {
                RepoListScreen(
                    onAddRepo = { nav.navigate(Routes.ADD_REPO) },
                    onOpenRepo = ::openRepo,
                    onOpenDrawer = ::openDrawer,
                    onOpenConflicts = { repoId -> nav.navigate(Routes.conflicts(repoId)) },
                    onOpenSearch = ::openSearch,
                )
            }
            composable(Routes.ADD_REPO) {
                AddRepoScreen(
                    onBack = { nav.popBackStack() },
                    onAdded = { id ->
                        if (id > 0L) container.settings.setDefaultRepo(id)
                        redirected = true
                        if (id > 0L) {
                            nav.navigate(Routes.browse(id)) {
                                popUpTo(Routes.REPOS) { inclusive = false }
                            }
                        } else {
                            nav.popBackStack()
                        }
                    },
                )
            }
            composable(
                Routes.BROWSE,
                arguments = listOf(navArgument("repoId") { type = NavType.LongType }),
            ) { entry ->
                val repoId = entry.arguments?.getLong("repoId") ?: return@composable
                BrowseScreen(
                    repoId = repoId,
                    onOpenDrawer = ::openDrawer,
                    onOpenNode = { kind, relPath ->
                        when (kind) {
                            FileKind.MD -> nav.navigate(Routes.mdView(repoId, relPath))
                            FileKind.DRAWIO -> nav.navigate(Routes.drawio(repoId, relPath))
                            FileKind.TEXT, FileKind.CODE -> nav.navigate(Routes.codeView(repoId, relPath))
                            else -> Unit
                        }
                    },
                )
            }
            composable(
                Routes.HOME_BOTH,
                arguments = listOf(
                    navArgument("repoId") { type = NavType.LongType },
                    navArgument("todoFirst") { type = NavType.IntType },
                ),
            ) { entry ->
                val repoId = entry.arguments?.getLong("repoId") ?: return@composable
                val todoFirst = entry.arguments?.getInt("todoFirst") == 1
                HomePagerScreen(
                    repoId = repoId,
                    todoFirst = todoFirst,
                    drawerOpen = drawerState.isOpen,
                    onOpenDrawer = ::openDrawer,
                    onOpenNode = { kind, relPath -> openDocByKind(repoId, relPath, kind) },
                    onOpenClassify = { nav.navigate(Routes.TODO_CLASSIFY) },
                    onCreateTodo = { nav.navigate(Routes.todoEditor()) },
                    onEditTodo = { id -> nav.navigate(Routes.todoEditor(id)) },
                )
            }
            composable(
                Routes.MD_VIEW,
                arguments = listOf(
                    navArgument("repoId") { type = NavType.LongType },
                    navArgument("relPath") { type = NavType.StringType },
                ),
            ) { entry ->
                val repoId = entry.arguments?.getLong("repoId") ?: return@composable
                val relPath = entry.arguments?.getString("relPath") ?: return@composable
                MdViewScreen(
                    repoId = repoId,
                    relPath = Routes.decode(relPath),
                    onBack = { nav.popBackStack() },
                    onOpenTodo = { id, path -> nav.navigate(Routes.todoList(id, path)) },
                    onOpenDoc = { id, path ->
                        openDocByKind(id, path, FileTypes.kind(path.substringAfterLast('/')))
                    },
                    onOpenMermaid = { id, path, idx -> nav.navigate(Routes.mermaid(id, path, idx)) },
                )
            }
            composable(
                Routes.TODO_LIST,
                arguments = listOf(
                    navArgument("repoId") { type = NavType.LongType },
                    navArgument("relPath") { type = NavType.StringType },
                ),
            ) { entry ->
                val repoId = entry.arguments?.getLong("repoId") ?: return@composable
                val relPath = entry.arguments?.getString("relPath") ?: return@composable
                TodoListScreen(
                    repoId = repoId,
                    relPath = Routes.decode(relPath),
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                Routes.CODE_VIEW,
                arguments = listOf(
                    navArgument("repoId") { type = NavType.LongType },
                    navArgument("relPath") { type = NavType.StringType },
                ),
            ) { entry ->
                val repoId = entry.arguments?.getLong("repoId") ?: return@composable
                val relPath = entry.arguments?.getString("relPath") ?: return@composable
                MdViewScreen(
                    repoId = repoId,
                    relPath = Routes.decode(relPath),
                    onBack = { nav.popBackStack() },
                    onOpenTodo = { _, _ -> },
                    markdownMode = false,
                )
            }
            composable(
                Routes.DRAWIO,
                arguments = listOf(
                    navArgument("repoId") { type = NavType.LongType },
                    navArgument("relPath") { type = NavType.StringType },
                ),
            ) { entry ->
                val repoId = entry.arguments?.getLong("repoId") ?: return@composable
                val relPath = entry.arguments?.getString("relPath") ?: return@composable
                DrawioScreen(
                    repoId = repoId,
                    relPath = Routes.decode(relPath),
                    onBack = { nav.popBackStack() },
                    onOpenRawText = { id, rel -> nav.navigate(Routes.codeView(id, rel)) },
                )
            }
            composable(Routes.TODO_HUB) {
                TodoHubScreen(
                    onOpenDrawer = ::openDrawer,
                    onOpenClassify = { nav.navigate(Routes.TODO_CLASSIFY) },
                    onCreateTodo = { nav.navigate(Routes.todoEditor()) },
                    onEditTodo = { id -> nav.navigate(Routes.todoEditor(id)) },
                )
            }
            composable(Routes.TODO_CLASSIFY) {
                TodoClassifyScreen(onBack = { nav.popBackStack() })
            }
            composable(
                Routes.TODO_EDITOR,
                arguments = listOf(navArgument("todoId") { type = NavType.LongType }),
            ) { entry ->
                val todoId = entry.arguments?.getLong("todoId") ?: 0L
                TodoEditorScreen(
                    todoId = todoId,
                    onBack = { nav.popBackStack() },
                    onSaved = { nav.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onOpenDrawer = ::openDrawer)
            }
            composable(Routes.SEARCH) {
                com.xstar.notebook.ui.search.SearchScreen(
                    onBack = { nav.popBackStack() },
                    onOpenDoc = { id, rel, type ->
                        openDocByKind(
                            id,
                            rel,
                            when (type) {
                                "md" -> FileKind.MD
                                "text", "code" -> FileKind.CODE
                                "drawio" -> FileKind.DRAWIO
                                else -> FileKind.OTHER
                            },
                        )
                    },
                )
            }
            composable(
                Routes.CONFLICTS,
                arguments = listOf(navArgument("repoId") { type = NavType.LongType }),
            ) { entry ->
                val repoId = entry.arguments?.getLong("repoId") ?: return@composable
                com.xstar.notebook.ui.conflict.ConflictScreen(
                    repoId = repoId,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                Routes.MERMAID,
                arguments = listOf(
                    navArgument("repoId") { type = NavType.LongType },
                    navArgument("relPath") { type = NavType.StringType },
                    navArgument("blockIndex") { type = NavType.IntType },
                ),
            ) { entry ->
                val repoId = entry.arguments?.getLong("repoId") ?: return@composable
                val relPath = entry.arguments?.getString("relPath") ?: return@composable
                val blockIndex = entry.arguments?.getInt("blockIndex") ?: return@composable
                com.xstar.notebook.ui.mermaid.MermaidScreen(
                    repoId = repoId,
                    relPath = Routes.decode(relPath),
                    blockIndex = blockIndex,
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun AppDrawerContent(
    homeSubtitle: String,
    onOpenHome: () -> Unit,
    onManage: () -> Unit,
    onTodoHub: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    val config = LocalConfiguration.current
    val drawerWidth = ((config.screenWidthDp * 2f / 3f).toInt()).coerceAtLeast(240).coerceAtMost(420).dp
    ModalDrawerSheet(
        modifier = Modifier.fillMaxHeight().width(drawerWidth),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(BrandGradient)),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BrandTitleLine(
                        title = "XStar 笔记",
                        subtitle = "Git 驱动的笔记本",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Column(Modifier.padding(top = 8.dp)) {
                HomeDocumentRow(subtitle = homeSubtitle, onClick = onOpenHome)
                HorizontalDivider(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                NavItem(
                    icon = Icons.Rounded.Bookmarks,
                    label = "仓库管理",
                    onClick = onManage,
                )
                NavItem(
                    icon = Icons.Rounded.Checklist,
                    label = "TODO",
                    onClick = onTodoHub,
                )
                NavItem(
                    icon = Icons.Rounded.Search,
                    label = "搜索笔记",
                    onClick = onSearch,
                )
                NavItem(
                    icon = Icons.Rounded.Palette,
                    label = "设置",
                    onClick = onSettings,
                )
            }

            Spacer(Modifier.weight(1f))
            Text(
                "XStar 笔记 v1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.navigationBarsPadding().padding(20.dp),
            )
        }
    }
}

@Composable
private fun HomeDocumentRow(subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).background(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.shapes.medium,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Home,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                "首页文档",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = "进入首页",
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, contentDescription = null) },
        selected = false,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(),
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}
