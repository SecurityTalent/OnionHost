package com.onionhost.app.ui.websites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onionhost.app.database.entity.WebsiteEntity
import com.onionhost.app.repository.WebsiteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class WebsitesViewModel @Inject constructor(
    private val websiteRepository: WebsiteRepository
) : ViewModel() {

    val websites: StateFlow<List<WebsiteEntity>> = websiteRepository.getAllWebsitesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun renameWebsite(website: WebsiteEntity, newName: String) {
        viewModelScope.launch {
            websiteRepository.updateWebsite(website.copy(name = newName))
        }
    }

    fun deleteWebsite(websiteId: String) {
        viewModelScope.launch {
            websiteRepository.deleteWebsite(websiteId)
        }
    }

    fun duplicateWebsite(website: WebsiteEntity) {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val copy = website.copy(
                id = newId,
                name = "${website.name} (Copy)",
                onionAddress = "",
                isEnabled = false,
                createdAt = System.currentTimeMillis()
            )
            websiteRepository.insertWebsite(copy)
        }
    }

    fun toggleWebsiteState(website: WebsiteEntity) {
        viewModelScope.launch {
            if (website.isEnabled) {
                websiteRepository.disableAllWebsites()
            } else {
                websiteRepository.setActiveWebsite(website.id)
            }
        }
    }
}
