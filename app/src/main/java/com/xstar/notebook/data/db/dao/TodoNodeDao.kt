package com.xstar.notebook.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.xstar.notebook.data.db.entity.TodoNodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoNodeDao {
    @Query("SELECT * FROM todo_nodes ORDER BY updatedAt DESC, id ASC")
    fun observeAll(): Flow<List<TodoNodeEntity>>

    @Query("SELECT * FROM todo_nodes")
    suspend fun getAll(): List<TodoNodeEntity>

    @Query("SELECT * FROM todo_nodes WHERE id = :id")
    suspend fun getById(id: Long): TodoNodeEntity?

    @Insert
    suspend fun insert(node: TodoNodeEntity): Long

    @Update
    suspend fun update(node: TodoNodeEntity)

    @Query("DELETE FROM todo_nodes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM todo_nodes WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
