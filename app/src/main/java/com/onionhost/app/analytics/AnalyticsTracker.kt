package com.onionhost.app.analytics

import com.onionhost.app.database.dao.VisitorStatDao
import com.onionhost.app.database.dao.WebsiteDao
import com.onionhost.app.database.entity.VisitorStatEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnalyticsTracker(
    private val visitorStatDao: VisitorStatDao,
    private val websiteDao: WebsiteDao
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    suspend fun recordRequest(
        websiteId: String,
        path: String,
        statusCode: Int,
        bytesTransferred: Long,
        isDownload: Boolean
    ) {
        val todayStr = dateFormat.format(Date())
        val stat = VisitorStatEntity(
            websiteId = websiteId,
            requestedPath = path,
            statusCode = statusCode,
            bytesTransferred = bytesTransferred,
            timestamp = System.currentTimeMillis(),
            dateString = todayStr,
            isDownload = isDownload
        )

        visitorStatDao.insertStat(stat)
        websiteDao.incrementVisits(websiteId)

        if (isDownload) {
            websiteDao.incrementDownloads(websiteId)
        }
    }
}
