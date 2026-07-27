package com.onionhost.app.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LogLevel {
    INFO,
    WARNING,
    ERROR,
    TOR,
    HTTP
}

@Entity(tableName = "system_logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String
)
