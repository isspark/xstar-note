package com.xstar.notebook.`data`.db.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.xstar.notebook.`data`.db.entity.LocalTodoEntity
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
public class LocalTodoDao_Impl(
  __db: RoomDatabase,
) : LocalTodoDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfLocalTodoEntity: EntityInsertAdapter<LocalTodoEntity>

  private val __updateAdapterOfLocalTodoEntity: EntityDeleteOrUpdateAdapter<LocalTodoEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfLocalTodoEntity = object : EntityInsertAdapter<LocalTodoEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `local_todos` (`id`,`title`,`note`,`done`,`createdAt`,`updatedAt`) VALUES (nullif(?, 0),?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: LocalTodoEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.note)
        val _tmp: Int = if (entity.done) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        statement.bindLong(5, entity.createdAt)
        statement.bindLong(6, entity.updatedAt)
      }
    }
    this.__updateAdapterOfLocalTodoEntity = object : EntityDeleteOrUpdateAdapter<LocalTodoEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `local_todos` SET `id` = ?,`title` = ?,`note` = ?,`done` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: LocalTodoEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.note)
        val _tmp: Int = if (entity.done) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        statement.bindLong(5, entity.createdAt)
        statement.bindLong(6, entity.updatedAt)
        statement.bindLong(7, entity.id)
      }
    }
  }

  public override suspend fun insert(todo: LocalTodoEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfLocalTodoEntity.insertAndReturnId(_connection, todo)
    _result
  }

  public override suspend fun update(todo: LocalTodoEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfLocalTodoEntity.handle(_connection, todo)
  }

  public override fun observeAll(): Flow<List<LocalTodoEntity>> {
    val _sql: String = "SELECT * FROM local_todos ORDER BY done ASC, updatedAt DESC"
    return createFlow(__db, false, arrayOf("local_todos")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfDone: Int = getColumnIndexOrThrow(_stmt, "done")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<LocalTodoEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: LocalTodoEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          val _tmpDone: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfDone).toInt()
          _tmpDone = _tmp != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = LocalTodoEntity(_tmpId,_tmpTitle,_tmpNote,_tmpDone,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAll(): List<LocalTodoEntity> {
    val _sql: String = "SELECT * FROM local_todos"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfDone: Int = getColumnIndexOrThrow(_stmt, "done")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<LocalTodoEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: LocalTodoEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          val _tmpDone: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfDone).toInt()
          _tmpDone = _tmp != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = LocalTodoEntity(_tmpId,_tmpTitle,_tmpNote,_tmpDone,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: Long) {
    val _sql: String = "DELETE FROM local_todos WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
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
