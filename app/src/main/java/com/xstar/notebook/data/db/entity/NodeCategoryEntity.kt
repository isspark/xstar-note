package com.xstar.notebook.data.db.entity

import androidx.room.Entity

/** 节点 ↔ 分类标签 多对多。 */
@Entity(
    tableName = "node_categories",
    primaryKeys = ["nodeId", "categoryId"],
)
data class NodeCategoryEntity(
    val nodeId: Long,
    val categoryId: Long,
)
