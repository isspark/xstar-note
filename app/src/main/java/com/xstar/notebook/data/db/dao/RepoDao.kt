package com.xstar.notebook.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xstar.notebook.data.db.entity.RepoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RepoDao {
    @Query("SELECT * FROM repos ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RepoEntity>>

    @Query("SELECT * FROM repos WHERE id = :id")
    suspend fun getById(id: Long): RepoEntity?

    @Query("SELECT * FROM repos ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<RepoEntity>

    @Query("SELECT * FROM repos WHERE id = :id")
    fun observeById(id: Long): Flow<RepoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(repo: RepoEntity): Long

    @Update
    suspend fun update(repo: RepoEntity)

    @Query("DELETE FROM repos WHERE id = :id")
    suspend fun deleteById(id: Long)
}