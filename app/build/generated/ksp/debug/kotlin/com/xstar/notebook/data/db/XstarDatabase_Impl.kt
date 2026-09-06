package com.xstar.notebook.`data`.db

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.xstar.notebook.`data`.db.dao.DocDao
import com.xstar.notebook.`data`.db.dao.DocDao_Impl
import com.xstar.notebook.`data`.db.dao.LocalTodoDao
import com.xstar.notebook.`data`.db.dao.LocalTodoDao_Impl
import com.xstar.notebook.`data`.db.dao.RepoDao
import com.xstar.notebook.`data`.db.dao.RepoDao_Impl
import com.xstar.notebook.`data`.db.dao.TodoDao
import com.xstar.notebook.`data`.db.dao.TodoDao_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class XstarDatabase_Impl : XstarDatabase() {
  private val _repoDao: Lazy<RepoDao> = lazy {
    RepoDao_Impl(this)
  }

  private val _docDao: Lazy<DocDao> = lazy {
    DocDao_Impl(this)
  }

  private val _todoDao: Lazy<TodoDao> = lazy {
    TodoDao_Impl(this)
  }

  private val _localTodoDao: Lazy<LocalTodoDao> = lazy {
    LocalTodoDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(2, "d2a985e2849c2a4398255b6cd5eb7518", "519ede43ff9bd60103dc71a06c8167de") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `repos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `displayName` TEXT NOT NULL, `remoteUrl` TEXT NOT NULL, `hostType` TEXT NOT NULL, `username` TEXT NOT NULL, `defaultBranch` TEXT NOT NULL, `localDir` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `lastSyncAt` INTEGER, `syncState` TEXT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `docs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `repoId` INTEGER NOT NULL, `relPath` TEXT NOT NULL, `fileType` TEXT NOT NULL, `size` INTEGER NOT NULL, `modifiedAt` INTEGER NOT NULL, `rawText` TEXT, `checksumHash` TEXT)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_docs_repoId_relPath` ON `docs` (`repoId`, `relPath`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `todos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `repoId` INTEGER NOT NULL, `docRelPath` TEXT NOT NULL, `lineIndex` INTEGER NOT NULL, `title` TEXT NOT NULL, `checked` INTEGER NOT NULL, `position` INTEGER NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_todos_repoId_docRelPath` ON `todos` (`repoId`, `docRelPath`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `local_todos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `note` TEXT NOT NULL, `done` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'd2a985e2849c2a4398255b6cd5eb7518')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `repos`")
        connection.execSQL("DROP TABLE IF EXISTS `docs`")
        connection.execSQL("DROP TABLE IF EXISTS `todos`")
        connection.execSQL("DROP TABLE IF EXISTS `local_todos`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsRepos: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRepos.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRepos.put("displayName", TableInfo.Column("displayName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRepos.put("remoteUrl", TableInfo.Column("remoteUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRepos.put("hostType", TableInfo.Column("hostType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRepos.put("username", TableInfo.Column("username", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRepos.put("defaultBranch", TableInfo.Column("defaultBranch", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRepos.put("localDir", TableInfo.Column("localDir", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRepos.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRepos.put("lastSyncAt", TableInfo.Column("lastSyncAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRepos.put("syncState", TableInfo.Column("syncState", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRepos: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRepos: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoRepos: TableInfo = TableInfo("repos", _columnsRepos, _foreignKeysRepos, _indicesRepos)
        val _existingRepos: TableInfo = read(connection, "repos")
        if (!_infoRepos.equals(_existingRepos)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |repos(com.xstar.notebook.data.db.entity.RepoEntity).
              | Expected:
              |""".trimMargin() + _infoRepos + """
              |
              | Found:
              |""".trimMargin() + _existingRepos)
        }
        val _columnsDocs: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsDocs.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDocs.put("repoId", TableInfo.Column("repoId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDocs.put("relPath", TableInfo.Column("relPath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDocs.put("fileType", TableInfo.Column("fileType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDocs.put("size", TableInfo.Column("size", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDocs.put("modifiedAt", TableInfo.Column("modifiedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDocs.put("rawText", TableInfo.Column("rawText", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDocs.put("checksumHash", TableInfo.Column("checksumHash", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysDocs: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesDocs: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesDocs.add(TableInfo.Index("index_docs_repoId_relPath", true, listOf("repoId", "relPath"), listOf("ASC", "ASC")))
        val _infoDocs: TableInfo = TableInfo("docs", _columnsDocs, _foreignKeysDocs, _indicesDocs)
        val _existingDocs: TableInfo = read(connection, "docs")
        if (!_infoDocs.equals(_existingDocs)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |docs(com.xstar.notebook.data.db.entity.DocEntity).
              | Expected:
              |""".trimMargin() + _infoDocs + """
              |
              | Found:
              |""".trimMargin() + _existingDocs)
        }
        val _columnsTodos: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTodos.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTodos.put("repoId", TableInfo.Column("repoId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTodos.put("docRelPath", TableInfo.Column("docRelPath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTodos.put("lineIndex", TableInfo.Column("lineIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTodos.put("title", TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTodos.put("checked", TableInfo.Column("checked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTodos.put("position", TableInfo.Column("position", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTodos: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTodos: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesTodos.add(TableInfo.Index("index_todos_repoId_docRelPath", false, listOf("repoId", "docRelPath"), listOf("ASC", "ASC")))
        val _infoTodos: TableInfo = TableInfo("todos", _columnsTodos, _foreignKeysTodos, _indicesTodos)
        val _existingTodos: TableInfo = read(connection, "todos")
        if (!_infoTodos.equals(_existingTodos)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |todos(com.xstar.notebook.data.db.entity.TodoEntity).
              | Expected:
              |""".trimMargin() + _infoTodos + """
              |
              | Found:
              |""".trimMargin() + _existingTodos)
        }
        val _columnsLocalTodos: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsLocalTodos.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalTodos.put("title", TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalTodos.put("note", TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalTodos.put("done", TableInfo.Column("done", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalTodos.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalTodos.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysLocalTodos: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesLocalTodos: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoLocalTodos: TableInfo = TableInfo("local_todos", _columnsLocalTodos, _foreignKeysLocalTodos, _indicesLocalTodos)
        val _existingLocalTodos: TableInfo = read(connection, "local_todos")
        if (!_infoLocalTodos.equals(_existingLocalTodos)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |local_todos(com.xstar.notebook.data.db.entity.LocalTodoEntity).
              | Expected:
              |""".trimMargin() + _infoLocalTodos + """
              |
              | Found:
              |""".trimMargin() + _existingLocalTodos)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "repos", "docs", "todos", "local_todos")
  }

  public override fun clearAllTables() {
    super.performClear(false, "repos", "docs", "todos", "local_todos")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(RepoDao::class, RepoDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(DocDao::class, DocDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(TodoDao::class, TodoDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(LocalTodoDao::class, LocalTodoDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun repoDao(): RepoDao = _repoDao.value

  public override fun docDao(): DocDao = _docDao.value

  public override fun todoDao(): TodoDao = _todoDao.value

  public override fun localTodoDao(): LocalTodoDao = _localTodoDao.value
}
