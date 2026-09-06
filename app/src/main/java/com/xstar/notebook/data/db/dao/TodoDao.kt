package com.xstar.notebook.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xstar.notebook.data.db.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos WHERE repoId = :repoId ORDER BY position ASC")
    fun observeByRepo(repoId: Long): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE repoId = :repoId AND docRelPath = :docRelPath ORDER BY position ASC")
    fun observeByDoc(repoId: Long, docRelPath: String): Flow<List<TodoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(todo: TodoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(todos: List<TodoEntity>)

    @Update
    suspend fun update(todo: TodoEntity)

    @Query("SELECT * FROM todos WHERE repoId = :repoId AND docRelPath = :docRelPath")
    suspend fun getByDoc(repoId: Long, docRelPath: String): List<TodoEntity>

    @Query("DELETE FROM todos WHERE repoId = :repoId AND docRelPath = :docRelPath")
    suspend fun deleteByDoc(repoId: Long, docRelPath: String)

    @Query("DELETE FROM todos WHERE repoId = :repoId")
    suspend fun deleteByRepo(repoId: Long)
}