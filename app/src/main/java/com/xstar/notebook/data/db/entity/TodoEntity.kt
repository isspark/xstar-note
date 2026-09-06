package com.xstar.notebook.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "todos",
    indices = [Index(value = ["repoId", "docRelPath"])],
)
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val repoId: Long,
    val docRelPath: String,
    val lineIndex: Int,
    val title: String,
    val checked: Boolean,
    val position: Int,
)