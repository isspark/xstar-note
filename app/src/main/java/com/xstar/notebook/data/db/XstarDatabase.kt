package com.xstar.notebook.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xstar.notebook.data.db.dao.CategoryDao
import com.xstar.notebook.data.db.dao.DocDao
import com.xstar.notebook.data.db.dao.InboxDao
import com.xstar.notebook.data.db.dao.RepoDao
import com.xstar.notebook.data.db.dao.TodoDao
import com.xstar.notebook.data.db.dao.TodoNodeDao
import com.xstar.notebook.data.db.entity.CategoryEntity
import com.xstar.notebook.data.db.entity.CategorySystemEntity
import com.xstar.notebook.data.db.entity.DocEntity
import com.xstar.notebook.data.db.entity.InboxItemEntity
import com.xstar.notebook.data.db.entity.NodeCategoryEntity
import com.xstar.notebook.data.db.entity.RepoEntity
import com.xstar.notebook.data.db.entity.TodoEntity
import com.xstar.notebook.data.db.entity.TodoNodeEntity

@Database(
    entities = [
        RepoEntity::class,
        DocEntity::class,
        TodoEntity::class,
        TodoNodeEntity::class,
        CategorySystemEntity::class,
        CategoryEntity::class,
        NodeCategoryEntity::class,
        InboxItemEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class XstarDatabase : RoomDatabase() {
    abstract fun repoDao(): RepoDao
    abstract fun docDao(): DocDao
    abstract fun todoDao(): TodoDao
    abstract fun todoNodeDao(): TodoNodeDao
    abstract fun categoryDao(): CategoryDao
    abstract fun inboxDao(): InboxDao

    companion object {
        @Volatile private var instance: XstarDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `local_todos` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `done` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * v2 → v3：本地 TODO 清单升级为层级树（todo_nodes），旧记录变为根级任务；
         * 新增分类系统（内置两套 + 可自建）与多对多标签表。
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1) 层级节点表
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `todo_nodes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `parentId` INTEGER NOT NULL,
                        `type` TEXT,
                        `title` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `done` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `todo_nodes`
                        (`id`, `parentId`, `type`, `title`, `note`, `done`, `createdAt`, `updatedAt`, `position`)
                    SELECT `id`, 0, 'TASK', `title`, `note`, `done`, `createdAt`, `updatedAt`, `id`
                    FROM `local_todos`
                    ORDER BY `id` ASC
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE IF EXISTS `local_todos`")

                // 2) 分类系统 + 分类
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `category_systems` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `builtInKey` TEXT,
                        `name` TEXT NOT NULL,
                        `enabled` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `systemId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `colorArgb` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `node_categories` (
                        `nodeId` INTEGER NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        PRIMARY KEY(`nodeId`, `categoryId`)
                    )
                    """.trimIndent(),
                )
                installBuiltIns(db)
            }
        }

        private fun installBuiltIns(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                INSERT OR IGNORE INTO `category_systems` (`id`, `builtInKey`, `name`, `enabled`, `position`) VALUES
                (1, 'quadrant', '重要 × 紧急', 1, 0),
                (2, 'state', '状态', 1, 1),
                (3, 'type', '类型', 1, 2)
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT OR IGNORE INTO `categories`
                    (`id`, `systemId`, `name`, `colorArgb`, `position`) VALUES
                (101, 1, '重要且紧急', ${hex(0xFFE5484D)}, 0),
                (102, 1, '重要不紧急', ${hex(0xFFF5A623)}, 1),
                (103, 1, '紧急不重要', ${hex(0xFF3E8ED0)}, 2),
                (104, 1, '不重要不紧急', ${hex(0xFF8A93A6)}, 3),
                (201, 2, '待办', ${hex(0xFF8A93A6)}, 0),
                (202, 2, '进行中', ${hex(0xFF3E8ED0)}, 1),
                (203, 2, '已阻塞', ${hex(0xFFE5484D)}, 2),
                (204, 2, '已完成', ${hex(0xFF00A896)}, 3),
                (301, 3, '故事', ${hex(0xFF6B4CE0)}, 0),
                (302, 3, '需求', ${hex(0xFF3E8ED0)}, 1),
                (303, 3, '任务', ${hex(0xFF00A896)}, 2)
                """.trimIndent(),
            )
        }

        /**
         * v3 → v4：本地 TODO 的"类型"从节点字段迁移为内置"类型"分类系统的标签；
         * 重建 todo_nodes（去掉 type 列）。
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 预置"类型"系统与分类
                installBuiltIns(db)
                // 旧 type 字段 → node_categories 标签
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `node_categories` (`nodeId`, `categoryId`)
                    SELECT `id`,
                           CASE `type`
                               WHEN 'STORY' THEN 301
                               WHEN 'REQUIREMENT' THEN 302
                               WHEN 'TASK' THEN 303
                           END
                    FROM `todo_nodes`
                    WHERE `type` IS NOT NULL
                    """.trimIndent(),
                )
                // 重建表去掉 type 列（兼容 API 26 不支持 DROP COLUMN）
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `todo_nodes_v4` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `parentId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `done` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `todo_nodes_v4`
                        (`id`, `parentId`, `title`, `note`, `done`, `createdAt`, `updatedAt`, `position`)
                    SELECT `id`, `parentId`, `title`, `note`, `done`, `createdAt`, `updatedAt`, `position`
                    FROM `todo_nodes`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE IF EXISTS `todo_nodes`")
                db.execSQL("ALTER TABLE `todo_nodes_v4` RENAME TO `todo_nodes`")
            }
        }

        /** v4 -> v5: TODO 完全扁平化，并新增可选截止时间。 */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `todo_nodes_v5` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `done` INTEGER NOT NULL,
                        `dueAt` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `todo_nodes_v5`
                        (`id`, `title`, `note`, `done`, `dueAt`, `createdAt`, `updatedAt`)
                    SELECT `id`, `title`, `note`, `done`, NULL, `createdAt`, `updatedAt`
                    FROM `todo_nodes`
                    ORDER BY `parentId` ASC, `position` ASC, `id` ASC
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE IF EXISTS `todo_nodes`")
                db.execSQL("ALTER TABLE `todo_nodes_v5` RENAME TO `todo_nodes`")
            }
        }

        /** v5 -> v6: TODO 增加可选开始时间。 */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `todo_nodes` ADD COLUMN `startAt` INTEGER")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `inbox_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `url` TEXT,
                        `status` TEXT NOT NULL,
                        `targetType` TEXT,
                        `targetId` INTEGER,
                        `targetPath` TEXT,
                        `source` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private fun hex(v: Long): String = "0x" + java.lang.Long.toHexString(v)

        /** 全新安装时预置内置分类系统（建库后、首次可用前执行）。 */
        private val BUILTIN_SEED = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                installBuiltIns(db)
            }
        }

        fun getInstance(context: Context): XstarDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    XstarDatabase::class.java,
                    "xstar.db",
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                )
                    .addCallback(BUILTIN_SEED)
                    .build().also { instance = it }
            }
    }
}
