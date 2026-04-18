package com.reelcount.app.domain.tracker

import com.reelcount.app.data.db.ReelSessionEntity
import com.reelcount.app.data.model.ScrollClassification
import com.reelcount.app.data.model.TargetApp
import com.reelcount.app.domain.classifier.ClassificationResult
import com.reelcount.app.domain.classifier.ScrollEventData
import java.util.UUID

enum class TrackerState {
    IDLE,
    TRACKING,
    IN_REELS,
    IN_FEED
}

class ScrollTracker {

    var currentState: TrackerState = TrackerState.IDLE
        private set

    private var currentSession: ReelSessionEntity? = null
    private var consecutiveUnknownCount = 0
    private var lastReelScrollTime: Long = 0L

    // Callback invoked when a session should be saved/updated
    var onSessionUpdate: ((ReelSessionEntity) -> Unit)? = null

    fun onAppOpened(app: TargetApp) {
        if (currentState == TrackerState.IDLE) {
            currentState = TrackerState.TRACKING
        }
    }

    fun onAppClosed() {
        endCurrentSession()
        currentState = TrackerState.IDLE
        currentSession = null
    }

    fun onScrollEvent(
        event: ScrollEventData,
        result: ClassificationResult,
        app: TargetApp
    ) {
        when (result.classification) {
            ScrollClassification.REEL -> handleReelScroll(event, app)
            ScrollClassification.FEED -> handleFeedScroll()
            ScrollClassification.UNKNOWN -> handleUnknownScroll()
        }
    }

    private fun handleReelScroll(event: ScrollEventData, app: TargetApp) {
        consecutiveUnknownCount = 0

        when (currentState) {
            TrackerState.IDLE, TrackerState.TRACKING, TrackerState.IN_FEED -> {
                // Start a new reel session
                endCurrentSession()
                startNewSession(app, event.timestamp)
                currentState = TrackerState.IN_REELS
            }
            TrackerState.IN_REELS -> {
                // Increment count in current session
                val session = currentSession ?: return
                val watchTime = if (lastReelScrollTime > 0) event.timestamp - lastReelScrollTime else 0L
                val updated = session.copy(
                    reelCount = session.reelCount + 1,
                    totalWatchTimeMs = session.totalWatchTimeMs + watchTime
                )
                currentSession = updated
                onSessionUpdate?.invoke(updated)
            }
        }
        lastReelScrollTime = event.timestamp
    }

    private fun handleFeedScroll() {
        consecutiveUnknownCount = 0
        when (currentState) {
            TrackerState.IN_REELS -> {
                endCurrentSession()
                currentState = TrackerState.IN_FEED
            }
            TrackerState.IDLE -> currentState = TrackerState.TRACKING
            else -> currentState = TrackerState.IN_FEED
        }
    }

    private fun handleUnknownScroll() {
        consecutiveUnknownCount++
        // Only act on unknown after 3+ consecutive unknowns — don't flip-flop state
        if (consecutiveUnknownCount >= 3 && currentState == TrackerState.IN_REELS) {
            endCurrentSession()
            currentState = TrackerState.TRACKING
        }
    }

    private fun startNewSession(app: TargetApp, startTime: Long) {
        val session = ReelSessionEntity(
            sessionId = UUID.randomUUID().toString(),
            startTime = startTime,
            endTime = 0L,  // 0 = active
            app = app,
            reelCount = 1,
            totalWatchTimeMs = 0L
        )
        currentSession = session
        onSessionUpdate?.invoke(session)
    }

    private fun endCurrentSession() {
        val session = currentSession ?: return
        if (session.endTime == 0L) {
            val ended = session.copy(endTime = System.currentTimeMillis())
            currentSession = ended
            onSessionUpdate?.invoke(ended)
        }
    }

    fun getCurrentReelCount(): Int = currentSession?.reelCount ?: 0
    fun getCurrentSession(): ReelSessionEntity? = currentSession
}
