package com.onionhost.app.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "visitor_stats")
data class VisitorStatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val websiteId: String,
    val requestedPath: String,
    val statusCode: Int,
    val bytesTransferred: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String, // e.g. "2026-07-27"
    val isDownload: Boolean = false
)
