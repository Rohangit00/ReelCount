package com.reelcount.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.reelcount.app.data.model.ScrollClassification
import com.reelcount.app.data.model.TargetApp

@Entity(tableName = "scroll_events")
data class ScrollEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val packageName: String,
    val targetApp: TargetApp,
    val scrollDeltaY: Int,
    val fromIndex: Int,
    val toIndex: Int,
    val sourceClassName: String,
    val classificationScore: Float,
    val classification: ScrollClassification,
    val sessionId: String
)

@Entity(tableName = "reel_sessions")
data class ReelSessionEntity(
    @PrimaryKey val sessionId: String,
    val startTime: Long,
    val endTime: Long,           // 0 = still active
    val app: TargetApp,
    val reelCount: Int,
    val totalWatchTimeMs: Long
)

@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey val date: String,   // "2026-04-18"
    val totalReels: Int,
    val instagramReels: Int,
    val youtubeShorts: Int,
    val totalWatchTimeMs: Long,
    val sessionCount: Int
)
