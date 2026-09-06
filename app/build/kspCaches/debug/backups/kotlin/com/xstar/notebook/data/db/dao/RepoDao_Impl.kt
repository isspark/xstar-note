package com.xstar.notebook.`data`.db.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.xstar.notebook.`data`.db.entity.RepoEntity
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
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class RepoDao_Impl(
  __db: RoomDatabase,
) : RepoDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfRepoEntity: EntityInsertAdapter<RepoEntity>

  private val __updateAdapterOfRepoEntity: EntityDeleteOrUpdateAdapter<RepoEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfRepoEntity = object : EntityInsertAdapter<RepoEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `repos` (`id`,`displayName`,`remoteUrl`,`hostType`,`username`,`defaultBranch`,`localDir`,`createdAt`,`lastSyncAt`,`syncState`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: RepoEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.displayName)
        statement.bindText(3, entity.remoteUrl)
        statement.bindText(4, entity.hostType)
        statement.bindText(5, entity.username)
        statement.bindText(6, entity.defaultBranch)
        statement.bindText(7, entity.localDir)
        statement.bindLong(8, entity.createdAt)
        val _tmpLastSyncAt: Long? = entity.lastSyncAt
        if (_tmpLastSyncAt == null) {
          statement.bindNull(9)
        } else {
          statement.bindLong(9, _tmpLastSyncAt)
        }
        statement.bindText(10, entity.syncState)
      }
    }
    this.__updateAdapterOfRepoEntity = object : EntityDeleteOrUpdateAdapter<RepoEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `repos` SET `id` = ?,`displayName` = ?,`remoteUrl` = ?,`hostType` = ?,`username` = ?,`defaultBranch` = ?,`localDir` = ?,`createdAt` = ?,`lastSyncAt` = ?,`syncState` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: RepoEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.displayName)
        statement.bindText(3, entity.remoteUrl)
        statement.bindText(4, entity.hostType)
        statement.bindText(5, entity.username)
        statement.bindText(6, entity.defaultBranch)
        statement.bindText(7, entity.localDir)
        statement.bindLong(8, entity.createdAt)
        val _tmpLastSyncAt: Long? = entity.lastSyncAt
        if (_tmpLastSyncAt == null) {
          statement.bindNull(9)
        } else {
          statement.bindLong(9, _tmpLastSyncAt)
        }
        statement.bindText(10, entity.syncState)
        statement.bindLong(11, entity.id)
      }
    }
  }

  public override suspend fun insert(repo: RepoEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfRepoEntity.insertAndReturnId(_connection, repo)
    _result
  }

  public override suspend fun update(repo: RepoEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfRepoEntity.handle(_connection, repo)
  }

  public override fun observeAll(): Flow<List<RepoEntity>> {
    val _sql: String = "SELECT * FROM repos ORDER BY createdAt DESC"
    return createFlow(__db, false, arrayOf("repos")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfDisplayName: Int = getColumnIndexOrThrow(_stmt, "displayName")
        val _columnIndexOfRemoteUrl: Int = getColumnIndexOrThrow(_stmt, "remoteUrl")
        val _columnIndexOfHostType: Int = getColumnIndexOrThrow(_stmt, "hostType")
        val _columnIndexOfUsername: Int = getColumnIndexOrThrow(_stmt, "username")
        val _columnIndexOfDefaultBranch: Int = getColumnIndexOrThrow(_stmt, "defaultBranch")
        val _columnIndexOfLocalDir: Int = getColumnIndexOrThrow(_stmt, "localDir")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfLastSyncAt: Int = getColumnIndexOrThrow(_stmt, "lastSyncAt")
        val _columnIndexOfSyncState: Int = getColumnIndexOrThrow(_stmt, "syncState")
        val _result: MutableList<RepoEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: RepoEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpDisplayName: String
          _tmpDisplayName = _stmt.getText(_columnIndexOfDisplayName)
          val _tmpRemoteUrl: String
          _tmpRemoteUrl = _stmt.getText(_columnIndexOfRemoteUrl)
          val _tmpHostType: String
          _tmpHostType = _stmt.getText(_columnIndexOfHostType)
          val _tmpUsername: String
          _tmpUsername = _stmt.getText(_columnIndexOfUsername)
          val _tmpDefaultBranch: String
          _tmpDefaultBranch = _stmt.getText(_columnIndexOfDefaultBranch)
          val _tmpLocalDir: String
          _tmpLocalDir = _stmt.getText(_columnIndexOfLocalDir)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpLastSyncAt: Long?
          if (_stmt.isNull(_columnIndexOfLastSyncAt)) {
            _tmpLastSyncAt = null
          } else {
            _tmpLastSyncAt = _stmt.getLong(_columnIndexOfLastSyncAt)
          }
          val _tmpSyncState: String
          _tmpSyncState = _stmt.getText(_columnIndexOfSyncState)
          _item = RepoEntity(_tmpId,_tmpDisplayName,_tmpRemoteUrl,_tmpHostType,_tmpUsername,_tmpDefaultBranch,_tmpLocalDir,_tmpCreatedAt,_tmpLastSyncAt,_tmpSyncState)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getById(id: Long): RepoEntity? {
    val _sql: String = "SELECT * FROM repos WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfDisplayName: Int = getColumnIndexOrThrow(_stmt, "displayName")
        val _columnIndexOfRemoteUrl: Int = getColumnIndexOrThrow(_stmt, "remoteUrl")
        val _columnIndexOfHostType: Int = getColumnIndexOrThrow(_stmt, "hostType")
        val _columnIndexOfUsername: Int = getColumnIndexOrThrow(_stmt, "username")
        val _columnIndexOfDefaultBranch: Int = getColumnIndexOrThrow(_stmt, "defaultBranch")
        val _columnIndexOfLocalDir: Int = getColumnIndexOrThrow(_stmt, "localDir")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfLastSyncAt: Int = getColumnIndexOrThrow(_stmt, "lastSyncAt")
        val _columnIndexOfSyncState: Int = getColumnIndexOrThrow(_stmt, "syncState")
        val _result: RepoEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpDisplayName: String
          _tmpDisplayName = _stmt.getText(_columnIndexOfDisplayName)
          val _tmpRemoteUrl: String
          _tmpRemoteUrl = _stmt.getText(_columnIndexOfRemoteUrl)
          val _tmpHostType: String
          _tmpHostType = _stmt.getText(_columnIndexOfHostType)
          val _tmpUsername: String
          _tmpUsername = _stmt.getText(_columnIndexOfUsername)
          val _tmpDefaultBranch: String
          _tmpDefaultBranch = _stmt.getText(_columnIndexOfDefaultBranch)
          val _tmpLocalDir: String
          _tmpLocalDir = _stmt.getText(_columnIndexOfLocalDir)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpLastSyncAt: Long?
          if (_stmt.isNull(_columnIndexOfLastSyncAt)) {
            _tmpLastSyncAt = null
          } else {
            _tmpLastSyncAt = _stmt.getLong(_columnIndexOfLastSyncAt)
          }
          val _tmpSyncState: String
          _tmpSyncState = _stmt.getText(_columnIndexOfSyncState)
          _result = RepoEntity(_tmpId,_tmpDisplayName,_tmpRemoteUrl,_tmpHostType,_tmpUsername,_tmpDefaultBranch,_tmpLocalDir,_tmpCreatedAt,_tmpLastSyncAt,_tmpSyncState)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllOnce(): List<RepoEntity> {
    val _sql: String = "SELECT * FROM repos ORDER BY createdAt DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfDisplayName: Int = getColumnIndexOrThrow(_stmt, "displayName")
        val _columnIndexOfRemoteUrl: Int = getColumnIndexOrThrow(_stmt, "remoteUrl")
        val _columnIndexOfHostType: Int = getColumnIndexOrThrow(_stmt, "hostType")
        val _columnIndexOfUsername: Int = getColumnIndexOrThrow(_stmt, "username")
        val _columnIndexOfDefaultBranch: Int = getColumnIndexOrThrow(_stmt, "defaultBranch")
        val _columnIndexOfLocalDir: Int = getColumnIndexOrThrow(_stmt, "localDir")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfLastSyncAt: Int = getColumnIndexOrThrow(_stmt, "lastSyncAt")
        val _columnIndexOfSyncState: Int = getColumnIndexOrThrow(_stmt, "syncState")
        val _result: MutableList<RepoEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: RepoEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpDisplayName: String
          _tmpDisplayName = _stmt.getText(_columnIndexOfDisplayName)
          val _tmpRemoteUrl: String
          _tmpRemoteUrl = _stmt.getText(_columnIndexOfRemoteUrl)
          val _tmpHostType: String
          _tmpHostType = _stmt.getText(_columnIndexOfHostType)
          val _tmpUsername: String
          _tmpUsername = _stmt.getText(_columnIndexOfUsername)
          val _tmpDefaultBranch: String
          _tmpDefaultBranch = _stmt.getText(_columnIndexOfDefaultBranch)
          val _tmpLocalDir: String
          _tmpLocalDir = _stmt.getText(_columnIndexOfLocalDir)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpLastSyncAt: Long?
          if (_stmt.isNull(_columnIndexOfLastSyncAt)) {
            _tmpLastSyncAt = null
          } else {
            _tmpLastSyncAt = _stmt.getLong(_columnIndexOfLastSyncAt)
          }
          val _tmpSyncState: String
          _tmpSyncState = _stmt.getText(_columnIndexOfSyncState)
          _item = RepoEntity(_tmpId,_tmpDisplayName,_tmpRemoteUrl,_tmpHostType,_tmpUsername,_tmpDefaultBranch,_tmpLocalDir,_tmpCreatedAt,_tmpLastSyncAt,_tmpSyncState)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeById(id: Long): Flow<RepoEntity?> {
    val _sql: String = "SELECT * FROM repos WHERE id = ?"
    return createFlow(__db, false, arrayOf("repos")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfDisplayName: Int = getColumnIndexOrThrow(_stmt, "displayName")
        val _columnIndexOfRemoteUrl: Int = getColumnIndexOrThrow(_stmt, "remoteUrl")
        val _columnIndexOfHostType: Int = getColumnIndexOrThrow(_stmt, "hostType")
        val _columnIndexOfUsername: Int = getColumnIndexOrThrow(_stmt, "username")
        val _columnIndexOfDefaultBranch: Int = getColumnIndexOrThrow(_stmt, "defaultBranch")
        val _columnIndexOfLocalDir: Int = getColumnIndexOrThrow(_stmt, "localDir")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfLastSyncAt: Int = getColumnIndexOrThrow(_stmt, "lastSyncAt")
        val _columnIndexOfSyncState: Int = getColumnIndexOrThrow(_stmt, "syncState")
        val _result: RepoEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpDisplayName: String
          _tmpDisplayName = _stmt.getText(_columnIndexOfDisplayName)
          val _tmpRemoteUrl: String
          _tmpRemoteUrl = _stmt.getText(_columnIndexOfRemoteUrl)
          val _tmpHostType: String
          _tmpHostType = _stmt.getText(_columnIndexOfHostType)
          val _tmpUsername: String
          _tmpUsername = _stmt.getText(_columnIndexOfUsername)
          val _tmpDefaultBranch: String
          _tmpDefaultBranch = _stmt.getText(_columnIndexOfDefaultBranch)
          val _tmpLocalDir: String
          _tmpLocalDir = _stmt.getText(_columnIndexOfLocalDir)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpLastSyncAt: Long?
          if (_stmt.isNull(_columnIndexOfLastSyncAt)) {
            _tmpLastSyncAt = null
          } else {
            _tmpLastSyncAt = _stmt.getLong(_columnIndexOfLastSyncAt)
          }
          val _tmpSyncState: String
          _tmpSyncState = _stmt.getText(_columnIndexOfSyncState)
          _result = RepoEntity(_tmpId,_tmpDisplayName,_tmpRemoteUrl,_tmpHostType,_tmpUsername,_tmpDefaultBranch,_tmpLocalDir,_tmpCreatedAt,_tmpLastSyncAt,_tmpSyncState)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: Long) {
    val _sql: String = "DELETE FROM repos WHERE id = ?"
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
