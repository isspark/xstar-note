package com.xstar.notebook.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_todos")
data class LocalTodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val done: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
