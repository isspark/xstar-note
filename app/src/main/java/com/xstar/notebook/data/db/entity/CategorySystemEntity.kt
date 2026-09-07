package com.xstar.notebook.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 分类系统：一组预设的分类（如"重要×紧急"四象限、状态看板）。用户可自建系统。
 * [builtInKey] 为 null 表示用户自定义系统。
 */
@Entity(tableName = "category_systems")
data class CategorySystemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val builtInKey: String? = null,
    val name: String,
    val enabled: Boolean = true,
    val position: Int = 0,
)
