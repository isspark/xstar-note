package com.xstar.notebook.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.xstar.notebook.data.db.entity.InboxItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InboxDao {
    @Query("SELECT * FROM inbox_items ORDER BY updatedAt DESC, id DESC")
    fun observeAll(): Flow<List<InboxItemEntity>>

    @Query("SELECT * FROM inbox_items WHERE status = 'pending' ORDER BY updatedAt DESC, id DESC")
    fun observePending(): Flow<List<InboxItemEntity>>

    @Query("SELECT COUNT(*) FROM inbox_items WHERE status = 'pending'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM inbox_items WHERE id = :id")
    suspend fun getById(id: Long): InboxItemEntity?

    @Insert
    suspend fun insert(item: InboxItemEntity): Long

    @Update
    suspend fun update(item: InboxItemEntity)

    @Query("DELETE FROM inbox_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}
