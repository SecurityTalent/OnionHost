package com.onionhost.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.onionhost.app.database.entity.LogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntity)

    @Query("SELECT * FROM system_logs ORDER BY timestamp DESC LIMIT 500")
    fun getRecentLogsFlow(): Flow<List<LogEntity>>

    @Query("DELETE FROM system_logs")
    suspend fun clearAllLogs()
}
