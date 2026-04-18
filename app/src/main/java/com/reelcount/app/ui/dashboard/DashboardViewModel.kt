package com.reelcount.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelcount.app.data.db.ReelSessionEntity
import com.reelcount.app.data.db.ScrollEventEntity
import com.reelcount.app.data.repository.ScrollRepository
import com.reelcount.app.service.ReelAccessibilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val todayReelCount: Int = 0,
    val todayTotalScrolls: Int = 0,
    val instagramReels: Int = 0,
    val youtubeShorts: Int = 0,
    val isServiceEnabled: Boolean = false,
    val recentEvents: List<ScrollEventEntity> = emptyList(),
    val todaySessions: List<ReelSessionEntity> = emptyList(),
    val classificationRate: Float = 0f   // % of scrolls that were classified (not UNKNOWN)
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: ScrollRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        // Combine all flows into a single UI state
        viewModelScope.launch {
            combine(
                repository.getTodayReelCount(),
                repository.getTodayTotalScrollCount(),
                repository.getTodayEvents(),
                repository.getRecentEvents(30),
                repository.getSessionsForToday()
            ) { reelCount, totalScrolls, todayEvents, recentEvents, sessions ->

                val instagramReels = todayEvents.count {
                    it.targetApp.packageName == "com.instagram.android" &&
                    it.classification.name == "REEL"
                }
                val youtubeShorts = reelCount - instagramReels

                val classified = todayEvents.count {
                    it.classification.name != "UNKNOWN"
                }
                val classificationRate = if (totalScrolls > 0) {
                    classified.toFloat() / totalScrolls
                } else 0f

                DashboardUiState(
                    todayReelCount = reelCount,
                    todayTotalScrolls = totalScrolls,
                    instagramReels = instagramReels.coerceAtLeast(0),
                    youtubeShorts = youtubeShorts.coerceAtLeast(0),
                    isServiceEnabled = ReelAccessibilityService.isRunning,
                    recentEvents = recentEvents,
                    todaySessions = sessions,
                    classificationRate = classificationRate
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun refreshServiceStatus() {
        _uiState.update { it.copy(isServiceEnabled = ReelAccessibilityService.isRunning) }
    }
}
