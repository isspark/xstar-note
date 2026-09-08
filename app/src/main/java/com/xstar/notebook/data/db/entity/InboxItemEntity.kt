package com.xstar.notebook.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inbox_items")
data class InboxItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String = "",
    val content: String = "",
    val url: String? = null,
    val status: String = STATUS_PENDING,
    val targetType: String? = null,
    val targetId: Long? = null,
    val targetPath: String? = null,
    val source: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
) {
    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_LINK = "link"
        const val TYPE_TODO = "todo"

        const val STATUS_PENDING = "pending"
        const val STATUS_CONVERTED = "converted"
        const val STATUS_ARCHIVED = "archived"
    }
}
