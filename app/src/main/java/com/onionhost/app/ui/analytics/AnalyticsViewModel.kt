package com.onionhost.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onionhost.app.database.dao.DailyCount
import com.onionhost.app.database.dao.PathCount
import com.onionhost.app.database.entity.WebsiteEntity
import com.onionhost.app.repository.AnalyticsRepository
import com.onionhost.app.repository.WebsiteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val websiteRepository: WebsiteRepository
) : ViewModel() {

    val activeWebsite: StateFlow<WebsiteEntity?> = websiteRepository.getActiveWebsiteFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val totalVisits: StateFlow<Long> = activeWebsite.flatMapLatest { website ->
        if (website != null) analyticsRepository.getTotalVisits(website.id) else flowOf(0L)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    val totalDownloads: StateFlow<Long> = activeWebsite.flatMapLatest { website ->
        if (website != null) analyticsRepository.getTotalDownloads(website.id) else flowOf(0L)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    val topFiles: StateFlow<List<PathCount>> = activeWebsite.flatMapLatest { website ->
        if (website != null) analyticsRepository.getMostRequestedFiles(website.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val dailyStats: StateFlow<List<DailyCount>> = activeWebsite.flatMapLatest { website ->
        if (website != null) analyticsRepository.getDailyVisitorStats(website.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
