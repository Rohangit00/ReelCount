package com.reelcount.app.ui.history

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reelcount.app.data.db.DailyStatsEntity
import com.reelcount.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.weeklyStats.isEmpty()) {
            EmptyHistoryState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary cards
                item {
                    SummaryRow(
                        totalReels = uiState.totalReelsAllTime,
                        bestDay = uiState.bestDay
                    )
                }

                // Bar chart
                item {
                    WeeklyBarChart(stats = uiState.weeklyStats)
                }

                // Section header
                item {
                    Text(
                        "Daily Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Day rows
                items(uiState.weeklyStats.sortedByDescending { it.date }) { day ->
                    DayRow(stats = day)
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(totalReels: Int, bestDay: DailyStatsEntity?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        SummaryCard(
            label = "7-Day Total",
            value = totalReels.toString(),
            emoji = "🎬",
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            label = "Best Day",
            value = bestDay?.let { "${it.totalReels} reels" } ?: "—",
            emoji = "🏆",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    emoji: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(emoji, fontSize = 28.sp)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = PurplePrimary
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyBarChart(stats: List<DailyStatsEntity>) {
    val maxReels = stats.maxOfOrNull { it.totalReels }?.coerceAtLeast(1) ?: 1

    // Animate bars in on first compose
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(
            1f,
            animationSpec = tween(durationMillis = 800, easing = EaseOutCubic)
        )
    }
    val progress by animProgress.asState()

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Last 7 Days",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))

            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(color = InstagramOrange, label = "Instagram")
                LegendDot(color = YoutubeRed, label = "YouTube")
            }

            Spacer(Modifier.height(16.dp))

            // Chart — sorted oldest first
            val sorted = stats.sortedBy { it.date }
            val igColor = InstagramOrange
            val ytColor = YoutubeRed

            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                drawBarChart(sorted, maxReels, progress, igColor, ytColor)
            }

            // Day labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                sorted.forEach { day ->
                    Text(
                        text = formatDayLabel(day.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawBarChart(
    stats: List<DailyStatsEntity>,
    maxReels: Int,
    progress: Float,
    igColor: Color,
    ytColor: Color
) {
    val barGroupWidth = size.width / stats.size
    val barWidth = barGroupWidth * 0.28f
    val gap = barWidth * 0.25f
    val chartHeight = size.height - 8.dp.toPx()

    stats.forEachIndexed { i, day ->
        val x = i * barGroupWidth + barGroupWidth / 2f - barWidth - gap / 2f

        // Instagram bar
        val igHeight = (day.instagramReels.toFloat() / maxReels) * chartHeight * progress
        if (igHeight > 0) {
            drawRoundRect(
                color = igColor,
                topLeft = Offset(x, chartHeight - igHeight),
                size = Size(barWidth, igHeight),
                cornerRadius = CornerRadius(6.dp.toPx())
            )
        }

        // YouTube bar
        val ytHeight = (day.youtubeShorts.toFloat() / maxReels) * chartHeight * progress
        val xYt = x + barWidth + gap
        if (ytHeight > 0) {
            drawRoundRect(
                color = ytColor,
                topLeft = Offset(xYt, chartHeight - ytHeight),
                size = Size(barWidth, ytHeight),
                cornerRadius = CornerRadius(6.dp.toPx())
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DayRow(stats: DailyStatsEntity) {
    val isToday = stats.date == LocalDate.now().toString()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isToday)
            PurpleContainer.copy(alpha = 0.7f)
        else
            MaterialTheme.colorScheme.surface,
        border = if (isToday)
            BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.5f))
        else null,
        tonalElevation = if (isToday) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Date column
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatFullDate(stats.date),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isToday) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PurplePrimary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "Today",
                                style = MaterialTheme.typography.labelSmall,
                                color = PurplePrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppCount(emoji = "📸", count = stats.instagramReels, color = InstagramOrange)
                    AppCount(emoji = "▶️", count = stats.youtubeShorts, color = YoutubeRed)
                }
            }

            // Total count
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    stats.totalReels.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = if (isToday) PurplePrimary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "reels",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AppCount(emoji: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(emoji, fontSize = 12.sp)
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyHistoryState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("📅", fontSize = 64.sp)
            Text(
                "No history yet",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Start watching Reels to see\nyour daily stats here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatDayLabel(date: String): String {
    return try {
        val local = LocalDate.parse(date)
        local.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    } catch (e: Exception) { date }
}

private fun formatFullDate(date: String): String {
    return try {
        val local = LocalDate.parse(date)
        local.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } catch (e: Exception) { date }
}
