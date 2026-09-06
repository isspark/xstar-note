package com.xstar.notebook.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xstar.notebook.data.db.entity.DocEntity

@Dao
interface DocDao {
    @Query("SELECT * FROM docs WHERE repoId = :repoId AND relPath = :relPath LIMIT 1")
    suspend fun getByPath(repoId: Long, relPath: String): DocEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(doc: DocEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(docs: List<DocEntity>)

    @Query("SELECT * FROM docs WHERE repoId = :repoId")
    suspend fun getAllByRepo(repoId: Long): List<DocEntity>

    @Query("DELETE FROM docs WHERE repoId = :repoId AND relPath IN (:paths)")
    suspend fun deletePaths(repoId: Long, paths: List<String>)

    @Query("DELETE FROM docs WHERE repoId = :repoId")
    suspend fun deleteByRepo(repoId: Long)
}
