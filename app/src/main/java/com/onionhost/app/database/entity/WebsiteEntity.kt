package com.onionhost.app.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WebsiteType {
    FOLDER,
    ZIP,
    SINGLE_FILE
}

@Entity(tableName = "websites")
data class WebsiteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val localPath: String,
    val websiteType: WebsiteType,
    val onionAddress: String = "",
    val port: Int = 8080,
    val isEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val totalVisits: Long = 0,
    val totalDownloads: Long = 0,
    val requiresAuth: Boolean = false,
    val authUsername: String = "",
    val authPasswordHash: String = ""
)
