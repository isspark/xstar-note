package com.xstar.notebook.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.xstar.notebook.data.db.entity.LocalTodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalTodoDao {
    @Query("SELECT * FROM local_todos ORDER BY done ASC, updatedAt DESC")
    fun observeAll(): Flow<List<LocalTodoEntity>>

    @Query("SELECT * FROM local_todos")
    suspend fun getAll(): List<LocalTodoEntity>

    @Insert
    suspend fun insert(todo: LocalTodoEntity): Long

    @Update
    suspend fun update(todo: LocalTodoEntity)

    @Query("DELETE FROM local_todos WHERE id = :id")
    suspend fun deleteById(id: Long)
}
