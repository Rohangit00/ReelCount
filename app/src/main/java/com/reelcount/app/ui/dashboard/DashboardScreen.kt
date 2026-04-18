package com.reelcount.app.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reelcount.app.data.db.ScrollEventEntity
import com.reelcount.app.data.model.ScrollClassification
import com.reelcount.app.data.model.TargetApp
import com.reelcount.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Refresh service status when screen appears
    LaunchedEffect(Unit) {
        viewModel.refreshServiceStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ReelCount",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Service Status Banner
            item {
                ServiceStatusBanner(
                    isEnabled = uiState.isServiceEnabled,
                    onEnableClick = onNavigateToSettings
                )
            }

            // Main reel counter card
            item {
                ReelCounterCard(reelCount = uiState.todayReelCount)
            }

            // App breakdown
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppBreakdownCard(
                        modifier = Modifier.weight(1f),
                        app = "Instagram",
                        count = uiState.instagramReels,
                        color = InstagramOrange,
                        icon = "📸"
                    )
                    AppBreakdownCard(
                        modifier = Modifier.weight(1f),
                        app = "YouTube",
                        count = uiState.youtubeShorts,
                        color = YoutubeRed,
                        icon = "▶️"
                    )
                }
            }

            // Stats row
            item {
                StatsRow(
                    totalScrolls = uiState.todayTotalScrolls,
                    sessionCount = uiState.todaySessions.size,
                    classificationRate = uiState.classificationRate
                )
            }

            // Today's sessions
            if (uiState.todaySessions.isNotEmpty()) {
                item {
                    Text(
                        "Today's Sessions",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(uiState.todaySessions.take(5)) { session ->
                    SessionCard(session = session)
                }
            }

            // Recent events (debug list)
            if (uiState.recentEvents.isNotEmpty()) {
                item {
                    Text(
                        "Recent Scroll Events",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(uiState.recentEvents.take(20)) { event ->
                    ScrollEventRow(event = event)
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ServiceStatusBanner(isEnabled: Boolean, onEnableClick: () -> Unit) {
    val backgroundColor = if (isEnabled)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    else
        MaterialTheme.colorScheme.error.copy(alpha = 0.12f)

    val borderColor = if (isEnabled)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    else
        MaterialTheme.colorScheme.error.copy(alpha = 0.4f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pulse dot
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_alpha"
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                        else MaterialTheme.colorScheme.error.copy(alpha = alpha)
                    )
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isEnabled) "Tracking Active" else "Tracking Disabled",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                )
                Text(
                    if (isEnabled) "Monitoring Instagram & YouTube"
                    else "Enable accessibility service to start counting",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isEnabled) {
                TextButton(onClick = onEnableClick) {
                    Text("Enable", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReelCounterCard(reelCount: Int) {
    // Animate counter
    val animatedCount by animateIntAsState(
        targetValue = reelCount,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 100f),
        label = "reelCount"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        PurpleContainer,
                        Color(0xFF1E1040)
                    )
                )
            )
            .border(
                1.dp,
                PurplePrimary.copy(alpha = 0.3f),
                RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "REELS TODAY",
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 3.sp,
                color = PurplePrimary.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = animatedCount.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (reelCount == 0) "No reels detected yet"
                else if (reelCount < 10) "That's pretty mindful 🙌"
                else if (reelCount < 30) "Getting scrolly 📱"
                else "Reel deep in it 🌀",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AppBreakdownCard(
    modifier: Modifier = Modifier,
    app: String,
    count: Int,
    color: Color,
    icon: String
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(icon, fontSize = 28.sp)
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                app,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatsRow(totalScrolls: Int, sessionCount: Int, classificationRate: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatChip(modifier = Modifier.weight(1f), label = "Total Scrolls", value = totalScrolls.toString())
        StatChip(modifier = Modifier.weight(1f), label = "Sessions", value = sessionCount.toString())
        StatChip(
            modifier = Modifier.weight(1f),
            label = "Classified",
            value = "${(classificationRate * 100).toInt()}%"
        )
    }
}

@Composable
private fun StatChip(modifier: Modifier = Modifier, label: String, value: String) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SessionCard(session: com.reelcount.app.data.db.ReelSessionEntity) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val duration = if (session.endTime > 0) {
        val mins = (session.endTime - session.startTime) / 60000
        "${mins}m"
    } else "Active"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (session.app == TargetApp.INSTAGRAM) "📸" else "▶️",
                fontSize = 22.sp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${session.app.name.lowercase().replaceFirstChar { it.uppercase() }} Session",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    timeFormat.format(Date(session.startTime)) + " · $duration",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = PurplePrimary.copy(alpha = 0.15f)
            ) {
                Text(
                    "${session.reelCount} reels",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = PurplePrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ScrollEventRow(event: ScrollEventEntity) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val (badgeColor, badgeText, emoji) = when (event.classification) {
        ScrollClassification.REEL -> Triple(NeonAccent, "REEL", "🎬")
        ScrollClassification.FEED -> Triple(Color(0xFF888888), "FEED", "📜")
        ScrollClassification.UNKNOWN -> Triple(Color(0xFFFFA500), "?", "❓")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(emoji, fontSize = 14.sp)
        Text(
            timeFormat.format(Date(event.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp)
        )
        Text(
            "Δ${event.scrollDeltaY}px",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(70.dp),
            maxLines = 1
        )
        Text(
            "%.2f".format(event.classificationScore),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(36.dp)
        )
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = badgeColor.copy(alpha = 0.15f)
        ) {
            Text(
                badgeText,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = badgeColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
