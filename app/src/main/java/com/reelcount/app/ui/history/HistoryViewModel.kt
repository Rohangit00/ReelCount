package com.reelcount.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelcount.app.data.db.DailyStatsEntity
import com.reelcount.app.data.repository.ScrollRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class HistoryUiState(
    val weeklyStats: List<DailyStatsEntity> = emptyList(),
    val totalReelsAllTime: Int = 0,
    val bestDay: DailyStatsEntity? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ScrollRepository
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = repository.getWeeklyStats()
        .map { stats ->
            HistoryUiState(
                weeklyStats = stats,
                totalReelsAllTime = stats.sumOf { it.totalReels },
                bestDay = stats.maxByOrNull { it.totalReels }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HistoryUiState()
        )
}
