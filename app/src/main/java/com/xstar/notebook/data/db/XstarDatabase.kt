package com.xstar.notebook.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xstar.notebook.data.db.dao.DocDao
import com.xstar.notebook.data.db.dao.LocalTodoDao
import com.xstar.notebook.data.db.dao.RepoDao
import com.xstar.notebook.data.db.dao.TodoDao
import com.xstar.notebook.data.db.entity.DocEntity
import com.xstar.notebook.data.db.entity.LocalTodoEntity
import com.xstar.notebook.data.db.entity.RepoEntity
import com.xstar.notebook.data.db.entity.TodoEntity

@Database(
    entities = [RepoEntity::class, DocEntity::class, TodoEntity::class, LocalTodoEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class XstarDatabase : RoomDatabase() {
    abstract fun repoDao(): RepoDao
    abstract fun docDao(): DocDao
    abstract fun todoDao(): TodoDao
    abstract fun localTodoDao(): LocalTodoDao

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

        fun getInstance(context: Context): XstarDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    XstarDatabase::class.java,
                    "xstar.db",
                ).addMigrations(MIGRATION_1_2)
                    .build().also { instance = it }
            }
    }
}
