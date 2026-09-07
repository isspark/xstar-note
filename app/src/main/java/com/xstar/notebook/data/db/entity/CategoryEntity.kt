package com.xstar.notebook.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 分类系统下的一个具体分类（标签）。[colorArgb] 为 0xFFRRGGBB。 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val systemId: Long,
    val name: String,
    val colorArgb: Long = 0xFF8A93A6,
    val position: Int = 0,
)
