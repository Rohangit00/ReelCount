package com.reelcount.app.domain.classifier

import android.view.accessibility.AccessibilityNodeInfo
import com.reelcount.app.data.model.ScrollClassification

// ─── Data classes ────────────────────────────────────────────────────────────

data class ScrollEventData(
    val timestamp: Long,
    val packageName: String,
    val scrollDeltaY: Int,
    val fromIndex: Int,
    val toIndex: Int,
    val sourceClassName: String
)

data class ClassificationContext(
    val screenHeight: Int,
    val lastScrollTime: Long?,
    val lastScrollDelta: Int?,
    val recentScrollDeltas: List<Int> = emptyList(),
    val burstEventCount: Int = 1  // how many scroll events fired in the last 500ms
)

data class ClassificationResult(
    val classification: ScrollClassification,
    val score: Float,
    val signalScores: Map<String, Float>
)

// ─── Signal interface ─────────────────────────────────────────────────────────

interface ScrollSignal {
    val name: String
    val weight: Float
    fun evaluate(event: ScrollEventData, context: ClassificationContext): Float
}

// ─── Signal 1: Snap / Scroll Distance ────────────────────────────────────────

class SnapScrollSignal(private val screenHeight: Int) : ScrollSignal {
    override val name = "SnapScroll"
    override val weight = 0.45f

    override fun evaluate(event: ScrollEventData, context: ClassificationContext): Float {
        val delta = Math.abs(event.scrollDeltaY).toFloat()
        if (delta == 0f) return 0f
        
        val ratio = delta / screenHeight
        
        return when {
            ratio >= 0.95f -> 1.0f
            ratio >= 0.90f -> 0.9f
            ratio >= 0.85f -> 0.85f
            ratio >= 0.80f -> 0.8f
            ratio >= 0.75f -> 0.75f
            ratio >= 0.70f -> 0.7f
            ratio >= 0.65f -> 0.65f
            ratio >= 0.60f -> 0.6f
            ratio >= 0.55f -> 0.55f
            ratio >= 0.50f -> 0.5f
            ratio >= 0.45f -> 0.45f
            ratio >= 0.40f -> 0.40f
            ratio >= 0.35f -> 0.35f
            else           -> 0.0f
        }
    }
}

// ─── Signal 2: Scroll Burst (Event Count per Gesture) ───────────────────────
//
// Insight: Reel swipes are a single discrete gesture → 1–3 scroll events max.
// Feed momentum scrolling fires 8–20+ events continuously.
// burstEventCount = number of scroll events received in the past 500ms.

class ScrollBurstSignal : ScrollSignal {
    override val name = "ScrollBurst"
    override val weight = 0.10f

    override fun evaluate(event: ScrollEventData, context: ClassificationContext): Float {
        val count = context.burstEventCount
        return when {
            count <= 2  -> 1.0f   // 1-2 events: clean snap gesture → Reel
            count <= 4  -> 0.8f   // 3-4 events: still likely a Reel
            count <= 7  -> 0.4f   // 5-7 events: ambiguous
            count <= 12 -> 0.1f   // 8-12 events: momentum feed scroll
            else        -> 0.0f   // 13+: definitely feed
        }
    }
}

// ─── Signal 3: Item Position Delta ───────────────────────────────────────────

class PositionDeltaSignal : ScrollSignal {
    override val name = "PositionDelta"
    override val weight = 0.15f

    override fun evaluate(event: ScrollEventData, context: ClassificationContext): Float {
        if (event.fromIndex < 0 || event.toIndex < 0) return 0.5f  // not available, neutral
        val delta = Math.abs(event.toIndex - event.fromIndex)
        return when (delta) {
            1    -> 1.0f   // single-page pager advance
            2    -> 0.3f   // fast swipe, skipped one
            0    -> 0.1f   // didn't change — small scroll
            else -> 0.0f   // > 2: definitely a list scroll
        }
    }
}

// ─── Signal 4: UI Hierarchy Inspection ───────────────────────────────────────

class UIHierarchySignal : ScrollSignal {
    override val name = "UIHierarchy"
    override val weight = 0.35f

    // Instagram Reels indicators
    private val REELS_CONTAINER_CLASSES = setOf(
        "androidx.viewpager2.widget.ViewPager2",
        "androidx.recyclerview.widget.RecyclerView"
    )
    private val INSTAGRAM_REELS_CONTENT_DESCRIPTIONS = setOf(
        "like", "comment", "share", "audio", "remix"  // lowercase match
    )
    private val FEED_INDICATOR_DESCRIPTIONS = setOf(
        "more options", "sponsored", "follow", "profile picture"
    )

    fun evaluate(event: ScrollEventData, nodeInfo: AccessibilityNodeInfo?, screenHeight: Int): Float {
        if (nodeInfo == null) return 0.5f  // can't determine, neutral

        return try {
            val score = inspectNode(nodeInfo, event.packageName, screenHeight)
            nodeInfo.recycle()
            score
        } catch (e: Exception) {
            0.5f  // errors during inspection → neutral
        }
    }

    // Default evaluate without node info — always neutral
    override fun evaluate(event: ScrollEventData, context: ClassificationContext): Float = 0.5f

    private fun inspectNode(root: AccessibilityNodeInfo, pkg: String, screenHeight: Int): Float {
        val className = root.className?.toString() ?: ""

        // Check if root is a vertical pager-like container
        val isPagerContainer = REELS_CONTAINER_CLASSES.any { className.contains(it, ignoreCase = true) }

        // Count children with reel-specific content descriptions
        val reelHints = countDescriptionMatches(root, INSTAGRAM_REELS_CONTENT_DESCRIPTIONS, depth = 0)
        val feedHints = countDescriptionMatches(root, FEED_INDICATOR_DESCRIPTIONS, depth = 0)

        // Check if child occupies full screen height
        val hasSingleFullScreenChild = checkFullScreenChild(root, screenHeight)

        // Scoring logic
        var score = 0.5f  // start neutral

        if (isPagerContainer) score += 0.15f
        if (hasSingleFullScreenChild) score += 0.20f
        if (reelHints > 0) score += 0.15f
        if (feedHints > reelHints) score -= 0.30f

        return score.coerceIn(0f, 1f)
    }

    private fun checkFullScreenChild(node: AccessibilityNodeInfo, screenHeight: Int): Boolean {
        if (node.childCount == 0) return false
        val child = node.getChild(0) ?: return false
        val rect = android.graphics.Rect()
        child.getBoundsInScreen(rect)
        child.recycle()
        val childHeight = rect.height()
        return childHeight >= screenHeight * 0.85f  // occupies 85%+ of screen
    }

    private fun countDescriptionMatches(
        node: AccessibilityNodeInfo,
        targets: Set<String>,
        depth: Int
    ): Int {
        if (depth > 8) return 0  // depth limit to avoid slow traversal
        var count = 0
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        if (targets.any { desc.contains(it) || text.contains(it) }) count++
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            count += countDescriptionMatches(child, targets, depth + 1)
            child.recycle()
        }
        return count
    }
}

// ─── Main Classifier ─────────────────────────────────────────────────────────

class ReelScrollClassifier(
    private val snapSignal: SnapScrollSignal,
    private val burstSignal: ScrollBurstSignal,
    private val positionDeltaSignal: PositionDeltaSignal,
    private val uiHierarchySignal: UIHierarchySignal,
    private val screenHeight: Int
) {
    companion object {
        const val REEL_THRESHOLD = 0.60f
        const val UNKNOWN_THRESHOLD = 0.40f
    }

    fun classify(
        event: ScrollEventData,
        nodeInfo: AccessibilityNodeInfo?,
        context: ClassificationContext
    ): ClassificationResult {
        val s1 = snapSignal.evaluate(event, context)
        val s2 = burstSignal.evaluate(event, context)
        val s3 = positionDeltaSignal.evaluate(event, context)
        val s4 = uiHierarchySignal.evaluate(event, nodeInfo, screenHeight)

        val totalWeight = snapSignal.weight + burstSignal.weight +
                positionDeltaSignal.weight + uiHierarchySignal.weight

        val score = (
            s1 * snapSignal.weight +
            s2 * burstSignal.weight +
            s3 * positionDeltaSignal.weight +
            s4 * uiHierarchySignal.weight
        ) / totalWeight

        val classification = when {
            score >= REEL_THRESHOLD    -> ScrollClassification.REEL
            score >= UNKNOWN_THRESHOLD -> ScrollClassification.UNKNOWN
            else                       -> ScrollClassification.FEED
        }

        return ClassificationResult(
            classification = classification,
            score = score,
            signalScores = mapOf(
                snapSignal.name to s1,
                burstSignal.name to s2,
                positionDeltaSignal.name to s3,
                uiHierarchySignal.name to s4
            )
        )
    }
}
