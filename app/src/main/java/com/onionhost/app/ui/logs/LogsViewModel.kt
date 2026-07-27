package com.onionhost.app.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onionhost.app.database.entity.LogEntity
import com.onionhost.app.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    val logs: StateFlow<List<LogEntity>> = analyticsRepository.getRecentLogs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun clearLogs() {
        viewModelScope.launch {
            analyticsRepository.clearLogs()
        }
    }
}
