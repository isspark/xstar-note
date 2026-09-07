# TODO 功能重构 · 方案与实施记录

> 对应代码改动：数据层 Room v6 + 扁平 TODO + 自由分类标签 + 起止时间 + 分类/月历双视图。
> 本文与 `docs/功能说明.md` 第 5 节保持一致。

## 1. 需求与决策（2026-09 确认）

| 决策点 | 结论 |
|---|---|
| 范围 | 只重构**本地 TODO**（抽屉 → TODO，独立于 Git）；文档内 `- [ ]` 复选框清单（doc 级 todos）保持原机制 |
| 层级 | **彻底扁平化**：所有旧节点保留为独立 TODO，不再维护父子关系 |
| 类型 | **类型 = 内置"类型"分类系统**（故事/需求/任务）的标签，可自建/改名/改色/删除，随标签参与筛选；节点字段 `type` 已移除 |
| 时间 | 每条可独立设置开始、截止日期 + 时刻；过期高亮；月历按起止区间覆盖日期 |
| 分类语义 | **自由多标签**：分类系统 = 一组带色分类；一条条目可同时挂多套系统、多个标签；"类型"系统内单选 |
| 内置分类系统 | ① 类型（故事/需求/任务）② 重要 × 紧急（四象限）③ 状态（待办/进行中/已阻塞/已完成）；支持自建系统/自定义分类 |
| 界面 | 默认按分类；筛选集中到浮动按钮；顶栏无文字图标隐式切换分类/月历；导入导出进入 ⋮ |
| 表单 | 新建/编辑为独立路由 `todos/edit/{todoId}`；标题/备注使用输入框标签，时间/分类使用上方小标题 |
| 形状 | 分类区域仅保留 8dp 外层圆角，内部用色块/分隔线；筛选与新增 FAB 统一默认 `XStarFab` 样式 |

## 2. 数据模型（Room v2 → v6）

| 表 | 字段 | 说明 |
|---|---|---|
| `todo_nodes` | id, title, note, done, startAt?, dueAt?, createdAt, updatedAt | 所有条目平级；时间为 epoch millis |
| `category_systems` | id, builtInKey?（null=自定义）, name, enabled, position | 内置 id=1(quadrant)、2(state)、3(type) 固定 |
| `categories` | id, systemId, name, colorArgb(Long 0xFFRRGGBB), position | 内置 101..104、201..204、301..303 |
| `node_categories` | (nodeId, categoryId) 联合主键 | 多对多 |

- **MIGRATION_2_3**：复制 `local_todos` → `todo_nodes`（根级、type='TASK'、保留勾选与时间），预置内置系统/分类。
- **MIGRATION_3_4**：种子"类型"系统；旧 `type` 字段(STORY/REQUIREMENT/TASK)→"类型"标签；重建 `todo_nodes` 去掉 type 列（兼容 API26 无 DROP COLUMN）。
- **MIGRATION_4_5**：保留全部节点与标签，重建 `todo_nodes` 去除 `parentId/position`，新增可空 `dueAt`。
- **MIGRATION_5_6**：通过兼容 API26 的 `ALTER TABLE` 无损增加可空 `startAt`。
- 内置数据源：`data/todo/TodoPresets.kt`（迁移 SQL 与代码共用固定 id）；新建库经 `RoomDatabase.Callback.onCreate` 种子化。

## 3. 代码结构

- 实体：`data/db/entity/`（TodoNodeEntity / CategorySystemEntity / CategoryEntity / NodeCategoryEntity）
- DAO：`data/db/dao/`（TodoNodeDao / CategoryDao）
- 纯计算：`viewmodel/TodoViews.kt`（标签索引、分组、完成态/过期筛选、月历日期聚合）
- 仓库：`data/repo/TodoHubRepository.kt`（扁平 CRUD、截止时间、标签、系统管理、Markdown 导入导出）
- 界面：`ui/todoHub/TodoHubScreen.kt`（主页）、`ui/todoHub/TodoClassifyScreen.kt`（分类管理，路由 `todos/classify`）
- 顶层聚合 Flow：`observeData()` → `TodoHubData(nodes, systems, categories, links)`

## 4. 视图计算要点

- 分类视图选择一个已启用分类系统；可展示全部分类卡片或只聚焦其中一个分类。
- 分类卡片使用分类色标题区与淡彩内容；系统内未打标签的 TODO 进入"未分类"。
- 日历按本地时区映射 LocalDate：只有 startAt 时显示开始日，只有 dueAt 时显示截止日，两者都有时覆盖起止区间内每天。
- 日历日期点按当前分类系统聚合分类颜色，同日最多显示 4 个不同颜色，未分类使用灰色。
- 过期定义：未完成且 dueAt 早于当前时刻。
- 类型仍是分类标签，编辑器内单选，自定义类型可直接参与分类展示。

## 5. 已知取舍（v1）

- 不做：拖拽排序、系统通知提醒、标签与勾选自动联动、标签写入仓库 Markdown。
- 内置系统整体不可改名/删除（只能停用），但其**分类可改名/改色/删除/新增**。
- 起止时间会写入 XStar 专用 Markdown 注释元数据；其它工具读取时不影响清单显示。

## 6. 实施状态

- [x] 数据层实体/DAO/Presets
- [x] DB v6 + MIGRATION_5_6（新增 startAt）
- [x] TodoHubRepository 重写（扁平 CRUD / 起止时间 / 标签 / Markdown 元数据）
- [x] TodoHubViewModel 重写 + TodoViews 派生
- [x] TodoHubScreen + TodoEditorScreen（独立编辑页 / 隐藏式视图切换 / 浮动筛选 / 分类色月历点）
- [x] TodoClassifyScreen（含内置系统分类调色与增删）+ 路由注册（`todos/classify`）
- [x] `gradlew :app:assembleDebug` 通过并已安装到设备
- [x] 同步文档（本文件 + `功能说明.md`）

## 7. 验证方式

1. `.\gradlew.bat :app:assembleDebug` 构建通过。
2. 真机/模拟器：升级 v4→v5 后旧节点全部保留为平级 TODO，类型/分类标签保留。
3. 新建 TODO 设置开始与截止日期/时刻；截止早于开始时不可保存，超过截止红色高亮。
4. 分类视图切换系统和系统内分类；未打标签条目进入"未分类"。
5. 跨日任务在月历起止区间每天有圆点且当天列表均展示；已过期与未设置日期列表正确。
6. 导出→导入保留勾选、备注、开始与截止时间，标题去重。

## 8. 首页模式

- `AppSettings.HomeMode`：`REPOSITORY`（默认）、`TODO`、`BOTH`（仓库在前）、`TODO_AND_REPOSITORY`（TODO 在前），保存于 `xstar_settings.home_mode`。
- 设置页可选择启动入口；修改后下次启动生效。
- 两种组合模式复用 `HomePagerScreen` / `HorizontalPager`，由 `todoFirst` 路由参数控制顺序；向左滑切换，并显示两个轻量页码点。
- TODO 模式不依赖仓库；仓库/组合模式无默认仓库时保持在仓库管理页。
- 抽屉首项命名为「首页文档」，点击根据 HomeMode 返回对应首页根路由。
- 组合 Pager 在左侧 28dp 边缘识别右滑并主动打开抽屉；抽屉打开时暂停 Pager 滑动，避免手势竞争。
- 启动重定向对仓库管理路由执行 inclusive popUpTo，防止系统返回手势把首页推出到仓库管理。
