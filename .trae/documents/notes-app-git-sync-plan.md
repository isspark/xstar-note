# xstar-notebook 笔记 APP（Git 同步）实现计划

## Context（背景）

工作目录 `d:\Code\android\xstar-notebook` 当前为空，是全新项目。用户要开发一款笔记 APP，底层用 git 同步文档仓库，支持 github / gitee / gitlab 三类远端。在 APP 上可查看仓库内文档，支持图片、drawio（.drawio）、Markdown（.md）等格式，并能基于 MD 内的 TODO 复选框（`- [ ]` / `- [x]`）生成清单，勾选后回写并推送远端。

**技术选型（已定默认）**：

- Android 原生：Kotlin + Jetpack Compose + MVVM，单 `app` 模块，包名 `com.xstar.notebook`。

- git 认证：Personal Access Token (PAT)，token 用 Android Keystore 加密存储。

- MVP 范围：仓库连接 + 拉取/推送 + MD 查看 + TODO 清单 + 图片；drawio 只做预览（WebView，离线）。

## 技术结论

| 设计点      | 结论                                                                                                                                                                                                                             |
| -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| JGit     | 官方 `org.eclipse.jgit:org.eclipse.jgit:6.10.x`，`minSdk = 26`；`SystemReader.setInstance(FileSystem.get().getUserConfigSystemReader())`；commit/push 前设置 local `user.name/email`；推送用 `UsernamePasswordCredentialsProvider` 注入 PAT。 |
| Markdown | Markwon 4.6.2 + `markwon-ext-tasklist` + `markwon-image-coil` + Coil 2.x（markwon-image-coil 绑定 Coil 2，勿用 Coil 3）。                                                                                                              |
| 图片       | Coil 2；仓库内相对路径图片自定义 `AsyncImageLoader` 从 `filesDir/repos/<repo>/...` 读文件。                                                                                                                                                      |
| Token    | `EncryptedSharedPreferences`（security-crypto，已废弃但 MVP 可用），加 Keystore 异常时的明文 fallback + UI 风险提示。                                                                                                                                |
| drawio   | WebView 加载 bundle 的 draw\.io viewer 静态资源，注入 `.drawio` XML 离线渲染（只读）。                                                                                                                                                            |
| 线程       | 所有 git/Room/文件 IO 走 `Dispatchers.IO`；每仓库一把 `Mutex` 串行化 JGit，避免 `index.lock` 冲突。                                                                                                                                                |

## 目录结构

```
xstar-notebook/
├── settings.gradle.kts / build.gradle.kts / gradle.properties / gradlew* / gradle/wrapper/
├── gradle/libs.versions.toml            # 版本目录，锁定全部依赖
└── app/
    ├── build.gradle.kts                 # AGP + JDK toolchain(17) + deps
    └── src/main/
        ├── AndroidManifest.xml          # INTERNET 权限
        ├── assets/drawio/               # bundle 的 draw.io viewer（阶段5）
        └── java/com/xstar/notebook/
            ├── XstarApplication.kt
            ├── MainActivity.kt
            ├── data/
            │   ├── db/                  # Room entity/dao/database
            │   ├── secure/              # SecureTokenStore
            │   ├── git/                 # GitClient / CredentialFactory
            │   └── repo/                # Repository（仓库域逻辑）
            ├── domain/model/            # Repo, Doc, TodoItem
            ├── ui/                      # navigation / repoList / addRepo / browse / mdView / drawioView / todoList / theme / components
            └── viewmodel/
```

## 存储方案

- **本地 checkout**：`context.filesDir/repos/<repoId>/`，目录名用独立 `repoId` 避免路径穿越。

- **Room** **`xstar.db`**：

  - `RepoEntity(id, displayName, remoteUrl, hostType, username, defaultBranch, localDir, createdAt, lastSyncAt, syncState)`

  - `DocEntity(id, repoId, relPath, type, size, modifiedAt, rawText?, checksumHash)` — 仅缓存被查看过的 md 文本加速渲染/TODO。

  - `TodoEntity(id, repoId, docRelPath, lineIndex, title, checked, position)`

- **Token**：只存 `EncryptedSharedPreferences`，key = `"pat_" + repoId`，不写进 `.git/config`（避免明文落盘），运行时注入 JGit Transport。

## 认证映射（PAT 作为"密码"）

- GitHub：`login = "x-access-token"`，password = token

- Gitee：`login = <gitee用户名>`，password = token

- GitLab：`login = "oauth2"`，password = token

## TODO 清单闭环

1. 逐行正则 `^(\s*)[-*]\s+\[([ xX])]\s+(.+)$` → `TodoEntity`。
2. TODO 列表页 Compose 复选框，勾选 `toggle(item)`。
3. 读原文件行数组，改第 `lineIndex` 行的 `[ ]`/`[x]`，`writeText(UTF_8)`。
4. `git.add(...)` → `commit(...)` → `push().setCredentialsProvider(cp)`。
5. push 前可选 `pull --rebase`；失败保留本地已改行（`syncState=dirty`）并提示，MVP 不做交互式冲突解决。

## UI 页面

- `/repos` 仓库列表：卡片 + 最近同步状态 + 添加入口。

- `/addRepo` 添加仓库：URL、host 类型单选、PAT、默认分支 → clone（含进度）。

- `/browse/{repoId}` 文件浏览：懒加载目录树，按扩展名显示图标。

- `/doc/{repoId}/{relPath}` MD 预览：MarkwonView + 图片；含 TODO 入口按钮。

- `/todo/{repoId}/{docRelPath}` TODO 列表：Room Flow 驱动 + 勾选回写。

- `/drawio/{repoId}/{relPath}` drawio 预览：WebView。

- 设置/同步控制：下拉刷新 = pull，显式 Push 按钮。

- Navigation 用 `navigation-compose`，路由用 Sealed class。

## 分阶段实施

- **阶段0 脚手架**：Gradle wrapper + version catalog + app 模块 + manifest + 空 Compose 壳。验证：`.\gradlew.bat assembleDebug` 出 APK。

- **阶段1 数据与安全层**：Room、SecureTokenStore、GitClient（clone/pull/push）、CredentialFactory、RepoRepository。验证：真机/模拟器 clone 一个测试仓库（含 md+png+drawio）。

- **阶段2 仓库管理 UI + clone 流**：仓库列表、添加仓库（验 token、进度）、拉取刷新。验证：添加测试仓库成功、错误 token 提示、重复进入不二次 clone。

- **阶段3 文件浏览 + MD/图片**：browse 树、Markwon 渲染、Coil 本地解析、图片全屏查看。验证：相对路径图片正常、`.md` 复选框只读渲染。

- **阶段4 TODO 闭环**：解析/勾选/回写/commit/push。验证：PC `git log` 确认 HEAD 与本地勾选一致。

- **阶段5 drawio 预览**：打包 viewer 资产 + WebView 注入 XML。验证：离线打开含图形/连线/中文的 `.drawio` 正确渲染。

- **阶段6 打磨**：同步状态、脏数据标识、加载/错误态、ProGuard rules（保留 JGit 反射类）。

## 关键风险

1. **JDK 24 vs AGP 8.x**：用 Java toolchain 固定到 17，或安装 JDK 17 配 `JAVA_HOME`。
2. **JGit Inflater 兼容**（部分 ROM）：纯文本 MVP 风险低，记录已知事项。
3. **security-crypto 已废弃**：MVP 可用，加 Keystore 异常 fallback。
4. **push 需 identity**：clone 后显式写 local config，否则 commit 抛错。
5. **drawio 资产体积**：先评估，过大则 MVP 降级为占位预览。

## 验证方式（端到端）

1. `.\gradlew.bat assembleDebug` 构建成功。
2. 在 github/gitee 建私有测试仓，推入 `README.md`（含 `- [ ]`/`- [x]`）、`img/a.png`、`flow.drawio`、`notes/a.md`。
3. App 添加仓库（输 PAT）→ filesDir 出现 clone，browse 可见全部文件。
4. 勾选 `README.md` TODO → push → PC `git pull` 确认一致。
5. 再在 PC 改一行 push，App 下拉 pull，确认反向同步；验证 drawio 离线渲染。

