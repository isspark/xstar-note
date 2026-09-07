package com.xstar.notebook.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_nodes")
data class TodoNodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val done: Boolean = false,
    /** 开始时间（epoch millis），null = 未设置 */
    val startAt: Long? = null,
    /** 截止时间（epoch millis），null = 未设置 */
    val dueAt: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
