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
    fun getStatsForDate(date: String): Flow<DailyStatsEntity?>   // query any date
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

        // Read current stats (or start from zero for this day)
        val current = dailyStatsDao.getStatsForDateOnce(today)
            ?: DailyStatsEntity(
                date = today,
                totalReels = 0,
                instagramReels = 0,
                youtubeShorts = 0,
                totalWatchTimeMs = 0L,
                sessionCount = 0
            )

        val updated = current.copy(
            totalReels = current.totalReels + 1,
            instagramReels = if (app == TargetApp.INSTAGRAM) current.instagramReels + 1 else current.instagramReels,
            youtubeShorts = if (app == TargetApp.YOUTUBE) current.youtubeShorts + 1 else current.youtubeShorts
        )

        dailyStatsDao.insertOrUpdate(updated)
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

    override fun getStatsForDate(date: String): Flow<DailyStatsEntity?> =
        dailyStatsDao.getStatsForDate(date)

    override fun getWeeklyStats(): Flow<List<DailyStatsEntity>> =
        dailyStatsDao.getLastSevenDays()

    override fun getRecentEvents(limit: Int): Flow<List<ScrollEventEntity>> =
        scrollEventDao.getRecentEvents(limit)

    override fun getSessionsForToday(): Flow<List<ReelSessionEntity>> =
        reelSessionDao.getSessionsForDate(today())
}
