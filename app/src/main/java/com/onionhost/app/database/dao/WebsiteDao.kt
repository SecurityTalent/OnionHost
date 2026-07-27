package com.onionhost.app.database.dao

import androidx.room.*
import com.onionhost.app.database.entity.WebsiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WebsiteDao {

    @Query("SELECT * FROM websites ORDER BY createdAt DESC")
    fun getAllWebsitesFlow(): Flow<List<WebsiteEntity>>

    @Query("SELECT * FROM websites ORDER BY createdAt DESC")
    suspend fun getAllWebsites(): List<WebsiteEntity>

    @Query("SELECT * FROM websites WHERE id = :id")
    suspend fun getWebsiteById(id: String): WebsiteEntity?

    @Query("SELECT * FROM websites WHERE isEnabled = 1 LIMIT 1")
    suspend fun getActiveWebsite(): WebsiteEntity?

    @Query("SELECT * FROM websites WHERE isEnabled = 1 LIMIT 1")
    fun getActiveWebsiteFlow(): Flow<WebsiteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWebsite(website: WebsiteEntity)

    @Update
    suspend fun updateWebsite(website: WebsiteEntity)

    @Query("DELETE FROM websites WHERE id = :id")
    suspend fun deleteWebsite(id: String)

    @Query("UPDATE websites SET isEnabled = CASE WHEN id = :targetId THEN 1 ELSE 0 END")
    suspend fun setActiveWebsite(targetId: String)

    @Query("UPDATE websites SET isEnabled = 0")
    suspend fun disableAllWebsites()

    @Query("UPDATE websites SET totalVisits = totalVisits + 1 WHERE id = :id")
    suspend fun incrementVisits(id: String)

    @Query("UPDATE websites SET totalDownloads = totalDownloads + 1 WHERE id = :id")
    suspend fun incrementDownloads(id: String)
}
