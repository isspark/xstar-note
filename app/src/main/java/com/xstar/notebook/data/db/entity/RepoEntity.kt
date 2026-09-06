package com.xstar.notebook.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repos")
data class RepoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val remoteUrl: String,
    val hostType: String,
    val username: String,
    val defaultBranch: String,
    val localDir: String,
    val createdAt: Long,
    val lastSyncAt: Long? = null,
    val syncState: String = "idle", // idle | syncing | dirty | error
)