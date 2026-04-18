package com.reelcount.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.reelcount.app.data.db.ScrollEventEntity
import com.reelcount.app.data.model.TargetApp
import com.reelcount.app.data.repository.ScrollRepository
import com.reelcount.app.domain.classifier.*
import com.reelcount.app.domain.tracker.ScrollTracker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class ReelAccessibilityService : AccessibilityService() {

    @Inject lateinit var repository: ScrollRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tracker = ScrollTracker()
    private lateinit var classifier: ReelScrollClassifier
    private val handler = Handler(Looper.getMainLooper())

    private var screenHeight: Int = 0
    private var lastScrollDelta: Int = 0
    private val recentDeltas = ArrayDeque<Int>(5)

    // ── Gesture buffering ──────────────────────────────────────────────────
    // We collect all scroll events during a gesture. When no new event arrives
    // for GESTURE_TIMEOUT_MS, we treat the gesture as complete and classify it
    // once with the total burst count.
    private val gestureBuffer = mutableListOf<BufferedScrollEvent>()
    private var classifyRunnable: Runnable? = null

    companion object {
        private const val GESTURE_TIMEOUT_MS = 300L   // silence threshold = gesture done
        val TARGET_PACKAGES = setOf(
            "com.instagram.android",
            "com.google.android.youtube"
        )

        @Volatile var isRunning = false
    }

    /** Lightweight holder for events waiting to be classified. */
    private data class BufferedScrollEvent(
        val timestamp: Long,
        val pkg: String,
        val app: TargetApp,
        val scrollDelta: Int,
        val fromIndex: Int,
        val toIndex: Int,
        val sourceClassName: String,
        val nodeInfo: AccessibilityNodeInfo?    // captured at event time
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        screenHeight = getScreenHeight()

        classifier = ReelScrollClassifier(
            snapSignal = SnapScrollSignal(screenHeight),
            burstSignal = ScrollBurstSignal(),
            positionDeltaSignal = PositionDeltaSignal(),
            uiHierarchySignal = UIHierarchySignal(),
            screenHeight = screenHeight
        )

        tracker.onSessionUpdate = { session ->
            serviceScope.launch { repository.upsertSession(session) }
        }

        android.util.Log.i("ReelService", "Service connected. Screen height: ${screenHeight}px")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in TARGET_PACKAGES) return

        val app = TargetApp.fromPackage(pkg)

        when (event.eventType) {

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (pkg in TARGET_PACKAGES) {
                    tracker.onAppOpened(app)
                } else {
                    tracker.onAppClosed()
                }
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                val now = System.currentTimeMillis()
                val scrollDelta = abs(event.scrollDeltaY)
                val nodeInfo = try { event.source } catch (_: Exception) { null }

                // Buffer this event
                gestureBuffer.add(BufferedScrollEvent(
                    timestamp = now,
                    pkg = pkg,
                    app = app,
                    scrollDelta = scrollDelta,
                    fromIndex = if (event.fromIndex >= 0) event.fromIndex else -1,
                    toIndex = if (event.toIndex >= 0) event.toIndex else -1,
                    sourceClassName = event.className?.toString() ?: "",
                    nodeInfo = nodeInfo
                ))

                // Reset the "gesture done" timer — every new event pushes it out
                classifyRunnable?.let { handler.removeCallbacks(it) }
                classifyRunnable = Runnable { onGestureComplete() }
                handler.postDelayed(classifyRunnable!!, GESTURE_TIMEOUT_MS)
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Future use
            }
        }
    }

    /**
     * Called when no new scroll event has arrived for GESTURE_TIMEOUT_MS.
     * The full gesture is now in [gestureBuffer]. We classify it once.
     */
    private fun onGestureComplete() {
        if (gestureBuffer.isEmpty()) return

        val events = gestureBuffer.toList()
        gestureBuffer.clear()

        val burstCount = events.size
        
        // Use the event with the largest scroll delta as the "representative" event
        val representative = events.maxByOrNull { it.scrollDelta } ?: return
        
        // Max single-event delta (for Snap signal — reel = one big snap)
        val maxDelta = representative.scrollDelta
        // Sum total delta (for logging only)
        val totalDelta = events.sumOf { it.scrollDelta }

        val eventData = ScrollEventData(
            timestamp = representative.timestamp,
            packageName = representative.pkg,
            scrollDeltaY = maxDelta,              // Snap evaluates the LARGEST single event
            fromIndex = events.first().fromIndex,
            toIndex = events.last().toIndex,
            sourceClassName = representative.sourceClassName
        )

        val context = ClassificationContext(
            screenHeight = screenHeight,
            lastScrollTime = null,
            lastScrollDelta = lastScrollDelta,
            recentScrollDeltas = recentDeltas.toList(),
            burstEventCount = burstCount
        )

        // Use the last event's node info (most likely to still be valid)
        val nodeInfo = events.lastOrNull()?.nodeInfo

        val result = classifier.classify(eventData, nodeInfo, context)

        val ss = result.signalScores
        android.util.Log.i("ReelService", 
            "[${representative.pkg}] Burst=${burstCount} max=${maxDelta}px sum=${totalDelta}px | " +
            "${result.classification} (${"%.2f".format(result.score)}) | " +
            "Snap=${"%.2f".format(ss["SnapScroll"] ?: 0f)} " +
            "Burst=${"%.2f".format(ss["ScrollBurst"] ?: 0f)} " +
            "Pos=${"%.2f".format(ss["PositionDelta"] ?: 0f)} " +
            "UI=${"%.2f".format(ss["UIHierarchy"] ?: 0f)}")

        tracker.onScrollEvent(eventData, result, representative.app)

        // Update rolling state
        lastScrollDelta = totalDelta
        if (recentDeltas.size >= 5) recentDeltas.removeFirst()
        recentDeltas.addLast(totalDelta)

        // Persist once per gesture
        serviceScope.launch {
            repository.recordScrollEvent(
                ScrollEventEntity(
                    timestamp = representative.timestamp,
                    packageName = representative.pkg,
                    targetApp = representative.app,
                    scrollDeltaY = totalDelta,
                    fromIndex = events.first().fromIndex,
                    toIndex = events.last().toIndex,
                    sourceClassName = representative.sourceClassName,
                    classificationScore = result.score,
                    classification = result.classification,
                    sessionId = tracker.getCurrentSession()?.sessionId ?: ""
                )
            )
        }

        // Recycle any remaining node infos
        events.forEach { buffered ->
            try { buffered.nodeInfo?.recycle() } catch (_: Exception) {}
        }
    }

    override fun onInterrupt() {
        isRunning = false
        tracker.onAppClosed()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        tracker.onAppClosed()
        classifyRunnable?.let { handler.removeCallbacks(it) }
        serviceScope.cancel()
    }

    private fun getScreenHeight(): Int {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            wm.currentWindowMetrics.bounds.height()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            metrics.heightPixels
        }
    }
}
