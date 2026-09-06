package com.xstar.notebook.`data`.db.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.xstar.notebook.`data`.db.entity.TodoEntity
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class TodoDao_Impl(
  __db: RoomDatabase,
) : TodoDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfTodoEntity: EntityInsertAdapter<TodoEntity>

  private val __updateAdapterOfTodoEntity: EntityDeleteOrUpdateAdapter<TodoEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfTodoEntity = object : EntityInsertAdapter<TodoEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `todos` (`id`,`repoId`,`docRelPath`,`lineIndex`,`title`,`checked`,`position`) VALUES (nullif(?, 0),?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TodoEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.repoId)
        statement.bindText(3, entity.docRelPath)
        statement.bindLong(4, entity.lineIndex.toLong())
        statement.bindText(5, entity.title)
        val _tmp: Int = if (entity.checked) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        statement.bindLong(7, entity.position.toLong())
      }
    }
    this.__updateAdapterOfTodoEntity = object : EntityDeleteOrUpdateAdapter<TodoEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `todos` SET `id` = ?,`repoId` = ?,`docRelPath` = ?,`lineIndex` = ?,`title` = ?,`checked` = ?,`position` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: TodoEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.repoId)
        statement.bindText(3, entity.docRelPath)
        statement.bindLong(4, entity.lineIndex.toLong())
        statement.bindText(5, entity.title)
        val _tmp: Int = if (entity.checked) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        statement.bindLong(7, entity.position.toLong())
        statement.bindLong(8, entity.id)
      }
    }
  }

  public override suspend fun upsert(todo: TodoEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfTodoEntity.insert(_connection, todo)
  }

  public override suspend fun upsertAll(todos: List<TodoEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfTodoEntity.insert(_connection, todos)
  }

  public override suspend fun update(todo: TodoEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfTodoEntity.handle(_connection, todo)
  }

  public override fun observeByRepo(repoId: Long): Flow<List<TodoEntity>> {
    val _sql: String = "SELECT * FROM todos WHERE repoId = ? ORDER BY position ASC"
    return createFlow(__db, false, arrayOf("todos")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, repoId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfRepoId: Int = getColumnIndexOrThrow(_stmt, "repoId")
        val _columnIndexOfDocRelPath: Int = getColumnIndexOrThrow(_stmt, "docRelPath")
        val _columnIndexOfLineIndex: Int = getColumnIndexOrThrow(_stmt, "lineIndex")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfChecked: Int = getColumnIndexOrThrow(_stmt, "checked")
        val _columnIndexOfPosition: Int = getColumnIndexOrThrow(_stmt, "position")
        val _result: MutableList<TodoEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TodoEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpRepoId: Long
          _tmpRepoId = _stmt.getLong(_columnIndexOfRepoId)
          val _tmpDocRelPath: String
          _tmpDocRelPath = _stmt.getText(_columnIndexOfDocRelPath)
          val _tmpLineIndex: Int
          _tmpLineIndex = _stmt.getLong(_columnIndexOfLineIndex).toInt()
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpChecked: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfChecked).toInt()
          _tmpChecked = _tmp != 0
          val _tmpPosition: Int
          _tmpPosition = _stmt.getLong(_columnIndexOfPosition).toInt()
          _item = TodoEntity(_tmpId,_tmpRepoId,_tmpDocRelPath,_tmpLineIndex,_tmpTitle,_tmpChecked,_tmpPosition)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeByDoc(repoId: Long, docRelPath: String): Flow<List<TodoEntity>> {
    val _sql: String = "SELECT * FROM todos WHERE repoId = ? AND docRelPath = ? ORDER BY position ASC"
    return createFlow(__db, false, arrayOf("todos")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, repoId)
        _argIndex = 2
        _stmt.bindText(_argIndex, docRelPath)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfRepoId: Int = getColumnIndexOrThrow(_stmt, "repoId")
        val _columnIndexOfDocRelPath: Int = getColumnIndexOrThrow(_stmt, "docRelPath")
        val _columnIndexOfLineIndex: Int = getColumnIndexOrThrow(_stmt, "lineIndex")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfChecked: Int = getColumnIndexOrThrow(_stmt, "checked")
        val _columnIndexOfPosition: Int = getColumnIndexOrThrow(_stmt, "position")
        val _result: MutableList<TodoEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TodoEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpRepoId: Long
          _tmpRepoId = _stmt.getLong(_columnIndexOfRepoId)
          val _tmpDocRelPath: String
          _tmpDocRelPath = _stmt.getText(_columnIndexOfDocRelPath)
          val _tmpLineIndex: Int
          _tmpLineIndex = _stmt.getLong(_columnIndexOfLineIndex).toInt()
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpChecked: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfChecked).toInt()
          _tmpChecked = _tmp != 0
          val _tmpPosition: Int
          _tmpPosition = _stmt.getLong(_columnIndexOfPosition).toInt()
          _item = TodoEntity(_tmpId,_tmpRepoId,_tmpDocRelPath,_tmpLineIndex,_tmpTitle,_tmpChecked,_tmpPosition)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getByDoc(repoId: Long, docRelPath: String): List<TodoEntity> {
    val _sql: String = "SELECT * FROM todos WHERE repoId = ? AND docRelPath = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, repoId)
        _argIndex = 2
        _stmt.bindText(_argIndex, docRelPath)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfRepoId: Int = getColumnIndexOrThrow(_stmt, "repoId")
        val _columnIndexOfDocRelPath: Int = getColumnIndexOrThrow(_stmt, "docRelPath")
        val _columnIndexOfLineIndex: Int = getColumnIndexOrThrow(_stmt, "lineIndex")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfChecked: Int = getColumnIndexOrThrow(_stmt, "checked")
        val _columnIndexOfPosition: Int = getColumnIndexOrThrow(_stmt, "position")
        val _result: MutableList<TodoEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TodoEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpRepoId: Long
          _tmpRepoId = _stmt.getLong(_columnIndexOfRepoId)
          val _tmpDocRelPath: String
          _tmpDocRelPath = _stmt.getText(_columnIndexOfDocRelPath)
          val _tmpLineIndex: Int
          _tmpLineIndex = _stmt.getLong(_columnIndexOfLineIndex).toInt()
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpChecked: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfChecked).toInt()
          _tmpChecked = _tmp != 0
          val _tmpPosition: Int
          _tmpPosition = _stmt.getLong(_columnIndexOfPosition).toInt()
          _item = TodoEntity(_tmpId,_tmpRepoId,_tmpDocRelPath,_tmpLineIndex,_tmpTitle,_tmpChecked,_tmpPosition)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteByDoc(repoId: Long, docRelPath: String) {
    val _sql: String = "DELETE FROM todos WHERE repoId = ? AND docRelPath = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, repoId)
        _argIndex = 2
        _stmt.bindText(_argIndex, docRelPath)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteByRepo(repoId: Long) {
    val _sql: String = "DELETE FROM todos WHERE repoId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, repoId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
