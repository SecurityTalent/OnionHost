package com.onionhost.app.repository

import com.onionhost.app.database.dao.DailyCount
import com.onionhost.app.database.dao.PathCount
import com.onionhost.app.database.dao.VisitorStatDao
import com.onionhost.app.database.entity.LogEntity
import com.onionhost.app.database.dao.LogDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface AnalyticsRepository {
    fun getTotalVisits(websiteId: String): Flow<Long>
    fun getTotalDownloads(websiteId: String): Flow<Long>
    fun getMostRequestedFiles(websiteId: String): Flow<List<PathCount>>
    fun getDailyVisitorStats(websiteId: String): Flow<List<DailyCount>>
    fun getRecentLogs(): Flow<List<LogEntity>>
    suspend fun clearLogs()
}

class AnalyticsRepositoryImpl @Inject constructor(
    private val visitorStatDao: VisitorStatDao,
    private val logDao: LogDao
) : AnalyticsRepository {

    override fun getTotalVisits(websiteId: String): Flow<Long> = visitorStatDao.getTotalVisitsCount(websiteId)

    override fun getTotalDownloads(websiteId: String): Flow<Long> = visitorStatDao.getTotalDownloadsCount(websiteId)

    override fun getMostRequestedFiles(websiteId: String): Flow<List<PathCount>> = visitorStatDao.getMostRequestedFiles(websiteId)

    override fun getDailyVisitorStats(websiteId: String): Flow<List<DailyCount>> = visitorStatDao.getDailyVisitorStats(websiteId)

    override fun getRecentLogs(): Flow<List<LogEntity>> = logDao.getRecentLogsFlow()

    override suspend fun clearLogs() = logDao.clearAllLogs()
}
