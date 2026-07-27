package com.onionhost.app.repository

import com.onionhost.app.database.entity.WebsiteEntity
import kotlinx.coroutines.flow.Flow

interface WebsiteRepository {
    fun getAllWebsitesFlow(): Flow<List<WebsiteEntity>>
    suspend fun getWebsiteById(id: String): WebsiteEntity?
    fun getActiveWebsiteFlow(): Flow<WebsiteEntity?>
    suspend fun insertWebsite(website: WebsiteEntity)
    suspend fun updateWebsite(website: WebsiteEntity)
    suspend fun deleteWebsite(id: String)
    suspend fun setActiveWebsite(id: String)
    suspend fun disableAllWebsites()
}
