package com.reelcount.app.data.db

import androidx.room.*
import com.reelcount.app.data.model.ScrollClassification
import kotlinx.coroutines.flow.Flow

@Dao
interface ScrollEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: ScrollEventEntity)

    @Query("SELECT * FROM scroll_events WHERE date(timestamp/1000, 'unixepoch', 'localtime') = :date ORDER BY timestamp DESC")
    fun getEventsForDate(date: String): Flow<List<ScrollEventEntity>>

    @Query("SELECT COUNT(*) FROM scroll_events WHERE date(timestamp/1000, 'unixepoch', 'localtime') = :date AND classification = 'REEL'")
    fun getReelCountForDate(date: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM scroll_events WHERE date(timestamp/1000, 'unixepoch', 'localtime') = :date")
    fun getTotalScrollCountForDate(date: String): Flow<Int>

    @Query("SELECT * FROM scroll_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int): Flow<List<ScrollEventEntity>>

    @Query("DELETE FROM scroll_events WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Query("SELECT * FROM scroll_events WHERE classification = :classification AND date(timestamp/1000, 'unixepoch', 'localtime') = :date")
    fun getEventsForDateByClassification(date: String, classification: ScrollClassification): Flow<List<ScrollEventEntity>>
}

@Dao
interface ReelSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ReelSessionEntity)

    @Update
    suspend fun updateSession(session: ReelSessionEntity)

    @Query("SELECT * FROM reel_sessions WHERE date(startTime/1000, 'unixepoch', 'localtime') = :date ORDER BY startTime DESC")
    fun getSessionsForDate(date: String): Flow<List<ReelSessionEntity>>

    @Query("SELECT * FROM reel_sessions WHERE endTime = 0 LIMIT 1")
    suspend fun getActiveSession(): ReelSessionEntity?

    @Query("SELECT * FROM reel_sessions ORDER BY startTime DESC LIMIT 1")
    suspend fun getLastSession(): ReelSessionEntity?
}

@Dao
interface DailyStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: DailyStatsEntity)

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    fun getStatsForDate(date: String): Flow<DailyStatsEntity?>  // reactive, for UI

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getStatsForDateOnce(date: String): DailyStatsEntity?  // one-shot, for write logic

    @Query("SELECT * FROM daily_stats WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getStatsForRange(startDate: String, endDate: String): Flow<List<DailyStatsEntity>>

    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT 7")
    fun getLastSevenDays(): Flow<List<DailyStatsEntity>>
}
