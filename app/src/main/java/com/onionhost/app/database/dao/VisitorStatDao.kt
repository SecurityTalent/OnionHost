package com.onionhost.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.onionhost.app.database.entity.VisitorStatEntity
import kotlinx.coroutines.flow.Flow

data class PathCount(
    val requestedPath: String,
    val requestCount: Int
)

data class DailyCount(
    val dateString: String,
    val count: Int
)

@Dao
interface VisitorStatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStat(stat: VisitorStatEntity)

    @Query("SELECT COUNT(*) FROM visitor_stats WHERE websiteId = :websiteId")
    fun getTotalVisitsCount(websiteId: String): Flow<Long>

    @Query("SELECT COUNT(*) FROM visitor_stats WHERE websiteId = :websiteId AND isDownload = 1")
    fun getTotalDownloadsCount(websiteId: String): Flow<Long>

    @Query("SELECT requestedPath, COUNT(*) as requestCount FROM visitor_stats WHERE websiteId = :websiteId GROUP BY requestedPath ORDER BY requestCount DESC LIMIT 10")
    fun getMostRequestedFiles(websiteId: String): Flow<List<PathCount>>

    @Query("SELECT dateString, COUNT(*) as count FROM visitor_stats WHERE websiteId = :websiteId GROUP BY dateString ORDER BY dateString DESC LIMIT 30")
    fun getDailyVisitorStats(websiteId: String): Flow<List<DailyCount>>

    @Query("DELETE FROM visitor_stats WHERE websiteId = :websiteId")
    suspend fun clearWebsiteStats(websiteId: String)
}
