package com.onionhost.app.repository

import com.onionhost.app.database.dao.WebsiteDao
import com.onionhost.app.database.entity.WebsiteEntity
import com.onionhost.app.storage.StorageManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WebsiteRepositoryImpl @Inject constructor(
    private val websiteDao: WebsiteDao,
    private val storageManager: StorageManager
) : WebsiteRepository {

    override fun getAllWebsitesFlow(): Flow<List<WebsiteEntity>> = websiteDao.getAllWebsitesFlow()

    override suspend fun getWebsiteById(id: String): WebsiteEntity? = websiteDao.getWebsiteById(id)

    override fun getActiveWebsiteFlow(): Flow<WebsiteEntity?> = websiteDao.getActiveWebsiteFlow()

    override suspend fun insertWebsite(website: WebsiteEntity) = websiteDao.insertWebsite(website)

    override suspend fun updateWebsite(website: WebsiteEntity) = websiteDao.updateWebsite(website)

    override suspend fun deleteWebsite(id: String) {
        storageManager.deleteWebsiteDirectory(id)
        websiteDao.deleteWebsite(id)
    }

    override suspend fun setActiveWebsite(id: String) = websiteDao.setActiveWebsite(id)

    override suspend fun disableAllWebsites() = websiteDao.disableAllWebsites()
}
