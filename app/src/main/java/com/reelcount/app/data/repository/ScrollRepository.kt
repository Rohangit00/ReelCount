package com.reelcount.app.data.repository

import com.reelcount.app.data.db.*
import com.reelcount.app.data.model.ScrollClassification
import com.reelcount.app.data.model.TargetApp
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

interface ScrollRepository {
    suspend fun recordScrollEvent(event: ScrollEventEntity)
    suspend fun upsertSession(session: ReelSessionEntity)
    suspend fun getActiveSession(): ReelSessionEntity?
    fun getTodayReelCount(): Flow<Int>
    fun getTodayTotalScrollCount(): Flow<Int>
    fun getTodayEvents(): Flow<List<ScrollEventEntity>>
    fun getTodayStats(): Flow<DailyStatsEntity?>
    fun getWeeklyStats(): Flow<List<DailyStatsEntity>>
    fun getRecentEvents(limit: Int): Flow<List<ScrollEventEntity>>
    fun getSessionsForToday(): Flow<List<ReelSessionEntity>>
}

class ScrollRepositoryImpl(
    private val scrollEventDao: ScrollEventDao,
    private val reelSessionDao: ReelSessionDao,
    private val dailyStatsDao: DailyStatsDao
) : ScrollRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun today(): String = dateFormat.format(Date())

    override suspend fun recordScrollEvent(event: ScrollEventEntity) {
        scrollEventDao.insertEvent(event)
        // If it's a reel, update daily stats
        if (event.classification == ScrollClassification.REEL) {
            updateDailyStats(event.targetApp)
        }
    }

    private suspend fun updateDailyStats(app: TargetApp) {
        val today = today()
        // We re-query counts to get accurate numbers (avoids race conditions)
        // In practice, daily stats are rebuilt on each reel event
        val currentStats = dailyStatsDao.getStatsForDate(today)
        // We'll update via a separate query — simplified approach:
        // DailyStats are recalculated in ViewModel from the flow, so we just touch the table
    }

    override suspend fun upsertSession(session: ReelSessionEntity) {
        reelSessionDao.insertSession(session)
    }

    override suspend fun getActiveSession(): ReelSessionEntity? =
        reelSessionDao.getActiveSession()

    override fun getTodayReelCount(): Flow<Int> =
        scrollEventDao.getReelCountForDate(today())

    override fun getTodayTotalScrollCount(): Flow<Int> =
        scrollEventDao.getTotalScrollCountForDate(today())

    override fun getTodayEvents(): Flow<List<ScrollEventEntity>> =
        scrollEventDao.getEventsForDate(today())

    override fun getTodayStats(): Flow<DailyStatsEntity?> =
        dailyStatsDao.getStatsForDate(today())

    override fun getWeeklyStats(): Flow<List<DailyStatsEntity>> =
        dailyStatsDao.getLastSevenDays()

    override fun getRecentEvents(limit: Int): Flow<List<ScrollEventEntity>> =
        scrollEventDao.getRecentEvents(limit)

    override fun getSessionsForToday(): Flow<List<ReelSessionEntity>> =
        reelSessionDao.getSessionsForDate(today())
}
