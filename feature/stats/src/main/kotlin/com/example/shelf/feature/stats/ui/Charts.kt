package com.example.shelf.feature.stats.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.shelf.core.model.GoalType
import com.example.shelf.feature.stats.DayProgress
import com.example.shelf.feature.stats.R
import java.time.format.TextStyle
import java.util.Locale

private const val FULL_CIRCLE = 360f
private const val START_ANGLE = -90f
private const val TRACK_ALPHA = 0.25f
private const val HEADROOM = 1.15f

/** Today's progress towards the goal as a ring (0..1), with the amount in the middle. */
@Composable
internal fun GoalRing(
    progress: Float,
    amount: Int,
    target: Int,
    goalType: GoalType,
    modifier: Modifier = Modifier,
    diameter: Dp = 148.dp,
) {
    val description = amountOfTarget(amount, target, goalType)
    val track = MaterialTheme.colorScheme.primary.copy(alpha = TRACK_ALPHA)
    val fill = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(diameter)
            .clearAndSetSemantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = size.minDimension * 0.1f
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, FULL_CIRCLE, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(
                color = fill,
                startAngle = START_ANGLE,
                sweepAngle = FULL_CIRCLE * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = amount.toString(), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = unitOfTarget(target, goalType),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Seven bars (oldest first) with a dashed goal line, drawn on a Canvas. The drawing is hidden from
 * accessibility services and replaced by a sentence listing every day's amount.
 */
@Composable
internal fun WeeklyBarChart(days: List<DayProgress>, target: Int, goalType: GoalType, modifier: Modifier = Modifier) {
    val locale = LocalLocale.current.platformLocale
    val description = weeklyDescription(days, target, goalType, locale)
    val metColor = MaterialTheme.colorScheme.primary
    // Outline keeps 3:1 against the card (WCAG 1.4.11) where a faded primary did not.
    val unmetColor = MaterialTheme.colorScheme.outline
    val goalLineColor = MaterialTheme.colorScheme.tertiary
    Column(
        modifier = modifier
            .testTag(StatsTestTags.WEEK_CHART)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
        ) {
            val maxValue = (maxOf(target, days.maxOfOrNull { it.amount } ?: 0) * HEADROOM).coerceAtLeast(1f)
            val slot = size.width / days.size
            val barWidth = slot * 0.55f
            val corner = CornerRadius(barWidth / 4, barWidth / 4)
            days.forEachIndexed { index, day ->
                val height = size.height * (day.amount / maxValue)
                drawRoundRect(
                    color = if (day.met) metColor else unmetColor,
                    topLeft = Offset(x = slot * index + (slot - barWidth) / 2, y = size.height - height),
                    size = Size(barWidth, height),
                    cornerRadius = corner,
                )
            }
            val goalY = size.height * (1 - target / maxValue)
            drawLine(
                color = goalLineColor,
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            days.forEach { day ->
                Text(
                    text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
internal fun amountOfTarget(amount: Int, target: Int, goalType: GoalType): String = when (goalType) {
    GoalType.PAGES -> pluralStringResource(R.plurals.stats_pages_of_goal, target, amount, target)
    GoalType.MINUTES -> pluralStringResource(R.plurals.stats_minutes_of_goal, target, amount, target)
}

@Composable
private fun unitOfTarget(target: Int, goalType: GoalType): String = when (goalType) {
    GoalType.PAGES -> pluralStringResource(R.plurals.stats_of_pages, target, target)
    GoalType.MINUTES -> pluralStringResource(R.plurals.stats_of_minutes, target, target)
}

@Composable
private fun weeklyDescription(days: List<DayProgress>, target: Int, goalType: GoalType, locale: Locale): String {
    val perDay = days.map { day ->
        val name = day.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        val amount = when (goalType) {
            GoalType.PAGES -> pluralStringResource(R.plurals.stats_amount_pages, day.amount, day.amount)
            GoalType.MINUTES -> pluralStringResource(R.plurals.stats_amount_minutes, day.amount, day.amount)
        }
        "$name $amount"
    }
    val goal = when (goalType) {
        GoalType.PAGES -> pluralStringResource(R.plurals.stats_amount_pages, target, target)
        GoalType.MINUTES -> pluralStringResource(R.plurals.stats_amount_minutes, target, target)
    }
    return stringResource(R.string.stats_week_description, perDay.joinToString(), goal)
}
