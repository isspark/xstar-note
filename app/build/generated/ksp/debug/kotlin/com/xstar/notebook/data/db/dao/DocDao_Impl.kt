package com.xstar.notebook.`data`.db.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.xstar.notebook.`data`.db.entity.DocEntity
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlin.text.StringBuilder

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class DocDao_Impl(
  __db: RoomDatabase,
) : DocDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfDocEntity: EntityInsertAdapter<DocEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfDocEntity = object : EntityInsertAdapter<DocEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `docs` (`id`,`repoId`,`relPath`,`fileType`,`size`,`modifiedAt`,`rawText`,`checksumHash`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: DocEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.repoId)
        statement.bindText(3, entity.relPath)
        statement.bindText(4, entity.fileType)
        statement.bindLong(5, entity.size)
        statement.bindLong(6, entity.modifiedAt)
        val _tmpRawText: String? = entity.rawText
        if (_tmpRawText == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpRawText)
        }
        val _tmpChecksumHash: String? = entity.checksumHash
        if (_tmpChecksumHash == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpChecksumHash)
        }
      }
    }
  }

  public override suspend fun upsert(doc: DocEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfDocEntity.insert(_connection, doc)
  }

  public override suspend fun upsertAll(docs: List<DocEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfDocEntity.insert(_connection, docs)
  }

  public override suspend fun getByPath(repoId: Long, relPath: String): DocEntity? {
    val _sql: String = "SELECT * FROM docs WHERE repoId = ? AND relPath = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, repoId)
        _argIndex = 2
        _stmt.bindText(_argIndex, relPath)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfRepoId: Int = getColumnIndexOrThrow(_stmt, "repoId")
        val _columnIndexOfRelPath: Int = getColumnIndexOrThrow(_stmt, "relPath")
        val _columnIndexOfFileType: Int = getColumnIndexOrThrow(_stmt, "fileType")
        val _columnIndexOfSize: Int = getColumnIndexOrThrow(_stmt, "size")
        val _columnIndexOfModifiedAt: Int = getColumnIndexOrThrow(_stmt, "modifiedAt")
        val _columnIndexOfRawText: Int = getColumnIndexOrThrow(_stmt, "rawText")
        val _columnIndexOfChecksumHash: Int = getColumnIndexOrThrow(_stmt, "checksumHash")
        val _result: DocEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpRepoId: Long
          _tmpRepoId = _stmt.getLong(_columnIndexOfRepoId)
          val _tmpRelPath: String
          _tmpRelPath = _stmt.getText(_columnIndexOfRelPath)
          val _tmpFileType: String
          _tmpFileType = _stmt.getText(_columnIndexOfFileType)
          val _tmpSize: Long
          _tmpSize = _stmt.getLong(_columnIndexOfSize)
          val _tmpModifiedAt: Long
          _tmpModifiedAt = _stmt.getLong(_columnIndexOfModifiedAt)
          val _tmpRawText: String?
          if (_stmt.isNull(_columnIndexOfRawText)) {
            _tmpRawText = null
          } else {
            _tmpRawText = _stmt.getText(_columnIndexOfRawText)
          }
          val _tmpChecksumHash: String?
          if (_stmt.isNull(_columnIndexOfChecksumHash)) {
            _tmpChecksumHash = null
          } else {
            _tmpChecksumHash = _stmt.getText(_columnIndexOfChecksumHash)
          }
          _result = DocEntity(_tmpId,_tmpRepoId,_tmpRelPath,_tmpFileType,_tmpSize,_tmpModifiedAt,_tmpRawText,_tmpChecksumHash)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllByRepo(repoId: Long): List<DocEntity> {
    val _sql: String = "SELECT * FROM docs WHERE repoId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, repoId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfRepoId: Int = getColumnIndexOrThrow(_stmt, "repoId")
        val _columnIndexOfRelPath: Int = getColumnIndexOrThrow(_stmt, "relPath")
        val _columnIndexOfFileType: Int = getColumnIndexOrThrow(_stmt, "fileType")
        val _columnIndexOfSize: Int = getColumnIndexOrThrow(_stmt, "size")
        val _columnIndexOfModifiedAt: Int = getColumnIndexOrThrow(_stmt, "modifiedAt")
        val _columnIndexOfRawText: Int = getColumnIndexOrThrow(_stmt, "rawText")
        val _columnIndexOfChecksumHash: Int = getColumnIndexOrThrow(_stmt, "checksumHash")
        val _result: MutableList<DocEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: DocEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpRepoId: Long
          _tmpRepoId = _stmt.getLong(_columnIndexOfRepoId)
          val _tmpRelPath: String
          _tmpRelPath = _stmt.getText(_columnIndexOfRelPath)
          val _tmpFileType: String
          _tmpFileType = _stmt.getText(_columnIndexOfFileType)
          val _tmpSize: Long
          _tmpSize = _stmt.getLong(_columnIndexOfSize)
          val _tmpModifiedAt: Long
          _tmpModifiedAt = _stmt.getLong(_columnIndexOfModifiedAt)
          val _tmpRawText: String?
          if (_stmt.isNull(_columnIndexOfRawText)) {
            _tmpRawText = null
          } else {
            _tmpRawText = _stmt.getText(_columnIndexOfRawText)
          }
          val _tmpChecksumHash: String?
          if (_stmt.isNull(_columnIndexOfChecksumHash)) {
            _tmpChecksumHash = null
          } else {
            _tmpChecksumHash = _stmt.getText(_columnIndexOfChecksumHash)
          }
          _item = DocEntity(_tmpId,_tmpRepoId,_tmpRelPath,_tmpFileType,_tmpSize,_tmpModifiedAt,_tmpRawText,_tmpChecksumHash)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deletePaths(repoId: Long, paths: List<String>) {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("DELETE FROM docs WHERE repoId = ")
    _stringBuilder.append("?")
    _stringBuilder.append(" AND relPath IN (")
    val _inputSize: Int = paths.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, repoId)
        _argIndex = 2
        for (_item: String in paths) {
          _stmt.bindText(_argIndex, _item)
          _argIndex++
        }
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteByRepo(repoId: Long) {
    val _sql: String = "DELETE FROM docs WHERE repoId = ?"
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
