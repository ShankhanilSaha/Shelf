package com.example.shelf.feature.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.GoalType
import com.example.shelf.core.ui.component.LoadingState
import com.example.shelf.core.ui.icon.ShelfIcons
import com.example.shelf.core.ui.preview.ThemePreviews
import com.example.shelf.core.ui.theme.ShelfTheme
import com.example.shelf.feature.stats.domain.StreakSummary
import com.example.shelf.feature.stats.ui.GoalRing
import com.example.shelf.feature.stats.ui.WeeklyBarChart
import com.example.shelf.feature.stats.ui.amountOfTarget
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun StatsScreen(
    uiState: StatsUiState,
    onLogReadingClick: () -> Unit,
    onDeleteLog: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.stats_title)) }) },
    ) { padding ->
        when (uiState) {
            StatsUiState.Loading -> LoadingState(Modifier.padding(padding))

            is StatsUiState.Success -> Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                StatsContent(state = uiState, onLogReadingClick = onLogReadingClick, onDeleteLog = onDeleteLog)
            }
        }
    }
}

@Composable
private fun StatsContent(state: StatsUiState.Success, onLogReadingClick: () -> Unit, onDeleteLog: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { TodayCard(state = state, onLogReadingClick = onLogReadingClick) }
        item { StatTiles(streak = state.streak, finishedThisYear = state.finishedThisYear) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle(stringResource(R.string.stats_this_week))
                    WeeklyBarChart(days = state.week, target = state.goal.target, goalType = state.goal.type)
                }
            }
        }
        item { SectionTitle(stringResource(R.string.stats_recent_sessions)) }
        if (state.recentLogs.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.stats_no_sessions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(state.recentLogs, key = {
                it.id
            }) { log -> RecentLogRow(log = log, onDelete = { onDeleteLog(log.id) }) }
        }
    }
}

@Composable
private fun TodayCard(state: StatsUiState.Success, onLogReadingClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GoalRing(
                    progress = state.todayProgress,
                    amount = state.todayAmount,
                    target = state.goal.target,
                    goalType = state.goal.type,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    SectionTitle(stringResource(R.string.stats_today))
                    Text(
                        text = amountOfTarget(state.todayAmount, state.goal.target, state.goal.type),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(
                            if (state.streak.todayMet) R.string.stats_goal_met else R.string.stats_goal_not_met,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Under the row, so the label stays on one line at large font sizes.
            Button(onClick = onLogReadingClick, modifier = Modifier.align(Alignment.End)) {
                Icon(ShelfIcons.Add, contentDescription = null)
                Text(stringResource(R.string.stats_log_reading), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatTiles(streak: StreakSummary, finishedThisYear: Int) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 3,
    ) {
        StatTile(
            icon = ShelfIcons.Streak,
            value = streak.current,
            label = pluralStringResource(R.plurals.stats_current_streak, streak.current),
            modifier = Modifier.weight(1f),
        )
        StatTile(
            icon = ShelfIcons.LongestStreak,
            value = streak.longest,
            label = pluralStringResource(R.plurals.stats_longest_streak, streak.longest),
            modifier = Modifier.weight(1f),
        )
        StatTile(
            icon = ShelfIcons.Book,
            value = finishedThisYear,
            label = pluralStringResource(R.plurals.stats_finished_this_year, finishedThisYear),
            modifier = Modifier.weight(1f),
        )
    }
}

/** One number with a label, read by TalkBack as a single phrase ("3, day streak"). */
@Composable
private fun StatTile(icon: ImageVector, value: Int, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text = value.toString(), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecentLogRow(log: RecentLog, onDelete: () -> Unit) {
    val locale = LocalLocale.current.platformLocale
    val dateText = remember(log.date, locale) {
        log.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    }
    val amounts = buildList {
        if (log.pages > 0) add(pluralStringResource(R.plurals.stats_amount_pages, log.pages, log.pages))
        if (log.minutes > 0) add(pluralStringResource(R.plurals.stats_amount_minutes, log.minutes, log.minutes))
    }.joinToString()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.bookTitle ?: stringResource(R.string.stats_session_no_book),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.stats_session_summary, dateText, amounts),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(ShelfIcons.Delete, contentDescription = stringResource(R.string.stats_delete_session))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
}

@ThemePreviews
@Composable
private fun StatsScreenPreview() {
    val today = LocalDate.of(2026, 11, 24)
    ShelfTheme(dynamicColor = false) {
        StatsScreen(
            uiState = StatsUiState.Success(
                goal = DailyGoal(GoalType.PAGES, 20),
                todayAmount = 12,
                streak = StreakSummary(current = 3, longest = 9, todayMet = false),
                finishedThisYear = 7,
                week = listOf(25, 0, 18, 30, 22, 20, 12).mapIndexed { index, amount ->
                    DayProgress(today.minusDays(6L - index), amount, amount >= 20)
                },
                recentLogs = listOf(RecentLog("1", today, 12, 0, "Piranesi")),
                today = today,
            ),
            onLogReadingClick = {},
            onDeleteLog = {},
        )
    }
}
