package com.onionhost.app.ui.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onionhost.app.database.entity.WebsiteEntity
import com.onionhost.app.database.entity.WebsiteType
import com.onionhost.app.hosting.OnionHostingService
import com.onionhost.app.repository.WebsiteRepository
import com.onionhost.app.storage.StorageManager
import com.onionhost.app.tor.TorManager
import com.onionhost.app.tor.TorStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SystemMetrics(
    val memoryUsageMb: Long = 0,
    val totalStorageAvailableMb: Long = 0,
    val networkSpeedKbps: Float = 0f,
    val hostingDurationSeconds: Long = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val websiteRepository: WebsiteRepository,
    private val torManager: TorManager,
    private val storageManager: StorageManager
) : ViewModel() {

    val activeWebsite: StateFlow<WebsiteEntity?> = websiteRepository.getActiveWebsiteFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val torStatus: StateFlow<TorStatus> = torManager.torStatus

    private val _systemMetrics = MutableStateFlow(SystemMetrics())
    val systemMetrics: StateFlow<SystemMetrics> = _systemMetrics.asStateFlow()

    init {
        updateMetrics()
    }

    private fun updateMetrics() {
        val runtime = Runtime.getRuntime()
        val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        _systemMetrics.value = SystemMetrics(
            memoryUsageMb = usedMem,
            totalStorageAvailableMb = 4096,
            networkSpeedKbps = 12.5f,
            hostingDurationSeconds = 3600
        )
    }

    fun startHosting(context: Context, websiteId: String) {
        OnionHostingService.startService(context, websiteId)
    }

    fun stopHosting(context: Context) {
        OnionHostingService.stopService(context)
    }

    fun importAndHost(context: Context, uri: Uri, type: WebsiteType) {
        viewModelScope.launch {
            val websiteId = UUID.randomUUID().toString()
            storageManager.importWebsiteContent(uri, type, websiteId)

            val newWebsite = WebsiteEntity(
                id = websiteId,
                name = "Site-${websiteId.take(6)}",
                localPath = websiteId,
                websiteType = type,
                isEnabled = true
            )
            websiteRepository.insertWebsite(newWebsite)
            websiteRepository.setActiveWebsite(websiteId)
            startHosting(context, websiteId)
        }
    }
}
