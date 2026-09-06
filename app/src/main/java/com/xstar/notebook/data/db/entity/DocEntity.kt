package com.xstar.notebook.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "docs",
    indices = [Index(value = ["repoId", "relPath"], unique = true)],
)
data class DocEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val repoId: Long,
    val relPath: String,
    val fileType: String,   // md | png | jpg | drawio | txt | ...
    val size: Long,
    val modifiedAt: Long,
    val rawText: String? = null,          // 缓存被查看过的 md 文本
    val checksumHash: String? = null,
)