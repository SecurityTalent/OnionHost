package com.onionhost.app.ui.home

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onionhost.app.database.entity.WebsiteEntity
import com.onionhost.app.database.entity.WebsiteType
import com.onionhost.app.hosting.OnionHostingService
import com.onionhost.app.http.AnonymousChatStore
import com.onionhost.app.repository.WebsiteRepository
import com.onionhost.app.storage.StorageManager
import com.onionhost.app.tor.TorManager
import com.onionhost.app.tor.TorStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
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

    private val selectedChatRoom = MutableStateFlow("")
    val activeChatRoom: StateFlow<String> = combine(activeWebsite, selectedChatRoom) { website, selected ->
        selected.ifBlank { website?.id?.let { "personal-$it" }.orEmpty() }
    }.stateIn(viewModelScope, SharingStarted.Lazily, "")

    val chatMessages: StateFlow<List<AnonymousChatStore.Message>> = activeChatRoom
        .flatMapLatest { room ->
            if (room.isBlank()) flowOf(emptyList()) else AnonymousChatStore.messagesFlow(room)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val chatRooms: StateFlow<List<String>> = AnonymousChatStore.roomNamesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    private val _chatActionInProgress = MutableStateFlow(false)
    val chatActionInProgress: StateFlow<Boolean> = _chatActionInProgress.asStateFlow()

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

    fun restartHosting(context: Context) {
        activeWebsite.value?.id?.let { OnionHostingService.restartService(context, it) }
    }

    fun sendChatMessage(text: String) {
        val room = activeChatRoom.value.ifBlank { return }
        val message = text.trim()
        if (message.isNotEmpty()) AnonymousChatStore.add(room, message)
    }

    fun sendChatAttachment(context: Context, text: String, uri: Uri) {
        val room = activeChatRoom.value.ifBlank { return }
        viewModelScope.launch(Dispatchers.IO) {
            _chatActionInProgress.value = true
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
                if (bytes.size > 5 * 1024 * 1024) return@launch
                val type = context.contentResolver.getType(uri) ?: return@launch
                val allowed = setOf("image/png", "image/jpeg", "image/gif", "image/webp", "video/mp4", "video/webm", "audio/mpeg", "audio/ogg", "audio/wav", "audio/webm")
                if (type !in allowed) return@launch
                val name = uri.lastPathSegment?.substringAfterLast('/')?.take(120) ?: "attachment"
                val data = "data:$type;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
                AnonymousChatStore.add(room, text.trim(), attachment = AnonymousChatStore.Attachment(data, name, type))
            } finally { _chatActionInProgress.value = false }
        }
    }

    fun deleteChatMessage(messageId: Long) {
        _chatActionInProgress.value = true
        activeChatRoom.value.takeIf { it.isNotBlank() }?.let { AnonymousChatStore.deleteByHost(it, messageId) }
        _chatActionInProgress.value = false
    }

    fun selectChatRoom(name: String) {
        selectedChatRoom.value = name.lowercase().filter { it.isLetterOrDigit() || it == '-' || it == '_' }.take(80)
    }

    fun createPrivateChatRoom() { selectedChatRoom.value = "personal-${UUID.randomUUID()}" }
    fun createRoom(name: String) {
        val cleanName = name.lowercase().filter { it.isLetterOrDigit() || it == '-' || it == '_' }.take(70)
        if (cleanName.isNotBlank()) selectedChatRoom.value = "room-$cleanName"
    }
    fun deleteActiveChatRoom() {
        activeChatRoom.value.takeIf { it.isNotBlank() }?.let(AnonymousChatStore::deleteRoom)
        selectedChatRoom.value = ""
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
