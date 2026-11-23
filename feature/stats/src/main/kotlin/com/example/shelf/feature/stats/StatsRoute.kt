package com.example.shelf.feature.stats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Entry point of the stats feature: the overview, or the "Log reading" sheet. */
@Composable
fun StatsRoute(
    navKey: StatsNavKey,
    onLogReadingClick: () -> Unit,
    onDismissLogReading: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (navKey) {
        StatsNavKey.Overview -> StatsOverviewRoute(onLogReadingClick = onLogReadingClick, modifier = modifier)
        StatsNavKey.LogReading -> LogReadingRoute(onDismiss = onDismissLogReading, modifier = modifier)
    }
}

@Composable
internal fun StatsOverviewRoute(
    onLogReadingClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(
        uiState = uiState,
        onLogReadingClick = onLogReadingClick,
        onDeleteLog = viewModel::onDeleteLog,
        modifier = modifier,
    )
}

@Composable
internal fun LogReadingRoute(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LogReadingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Saving closes the sheet: navigation is driven by state, not by a one-off event.
    LaunchedEffect(uiState) {
        if (uiState == LogReadingUiState.Saved) onDismiss()
    }
    val editing = uiState as? LogReadingUiState.Editing
    LogReadingSheet(
        readingBooks = editing?.readingBooks.orEmpty(),
        selectedWorkKey = viewModel.selectedWorkKey,
        pagesInput = viewModel.pagesInput,
        minutesInput = viewModel.minutesInput,
        date = viewModel.date,
        today = viewModel.today(),
        canSave = viewModel.canSave,
        onBookSelected = viewModel::onBookSelected,
        onPagesChange = viewModel::onPagesChange,
        onMinutesChange = viewModel::onMinutesChange,
        onDateChange = viewModel::onDateChange,
        onSave = viewModel::onSave,
        onCancel = onDismiss,
        modifier = modifier,
    )
}
