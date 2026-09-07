package com.xstar.notebook.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xstar.notebook.data.db.entity.CategoryEntity
import com.xstar.notebook.data.db.entity.CategorySystemEntity
import com.xstar.notebook.data.db.entity.NodeCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    // ---- 分类系统 ----
    @Query("SELECT * FROM category_systems ORDER BY position ASC, id ASC")
    fun observeSystems(): Flow<List<CategorySystemEntity>>

    @Query("SELECT * FROM category_systems ORDER BY position ASC, id ASC")
    suspend fun getAllSystems(): List<CategorySystemEntity>

    @Query("SELECT * FROM category_systems WHERE id = :id")
    suspend fun getSystemById(id: Long): CategorySystemEntity?

    @Insert
    suspend fun insertSystem(system: CategorySystemEntity): Long

    @Update
    suspend fun updateSystem(system: CategorySystemEntity)

    @Query("DELETE FROM category_systems WHERE id = :id")
    suspend fun deleteSystemById(id: Long)

    // ---- 分类（标签） ----
    @Query("SELECT * FROM categories ORDER BY systemId ASC, position ASC, id ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY position ASC, id ASC")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE systemId = :systemId ORDER BY position ASC, id ASC")
    suspend fun getCategoriesBySystem(systemId: Long): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Insert
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    @Query("DELETE FROM categories WHERE systemId = :systemId")
    suspend fun deleteCategoriesBySystem(systemId: Long)

    // ---- 节点↔分类 ----
    @Query("SELECT * FROM node_categories")
    fun observeNodeCategories(): Flow<List<NodeCategoryEntity>>

    @Query("SELECT * FROM node_categories")
    suspend fun getAllNodeCategories(): List<NodeCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNodeCategory(link: NodeCategoryEntity)

    @Query("DELETE FROM node_categories WHERE nodeId = :nodeId AND categoryId = :categoryId")
    suspend fun deleteNodeCategory(nodeId: Long, categoryId: Long)

    @Query("DELETE FROM node_categories WHERE nodeId IN (:nodeIds)")
    suspend fun deleteNodeCategories(nodeIds: List<Long>)

    @Query("DELETE FROM node_categories WHERE categoryId = :categoryId")
    suspend fun deleteNodeCategoriesByCategory(categoryId: Long)
}
