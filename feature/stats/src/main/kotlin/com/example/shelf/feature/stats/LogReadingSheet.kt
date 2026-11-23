package com.example.shelf.feature.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.shelf.core.model.ShelfBook
import com.example.shelf.core.ui.icon.ShelfIcons
import com.example.shelf.feature.stats.ui.StatsTestTags
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.example.shelf.core.ui.R as UiR

/** Content of the "Log reading" bottom sheet (the sheet itself is a navigation scene). */
@Composable
internal fun LogReadingSheet(
    readingBooks: List<ShelfBook>,
    selectedWorkKey: String?,
    pagesInput: String,
    minutesInput: String,
    date: LocalDate,
    today: LocalDate,
    canSave: Boolean,
    onBookSelected: (String?) -> Unit,
    onPagesChange: (String) -> Unit,
    onMinutesChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.stats_log_reading),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        BookPicker(books = readingBooks, selectedWorkKey = selectedWorkKey, onBookSelected = onBookSelected)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = pagesInput,
                onValueChange = onPagesChange,
                label = { Text(stringResource(R.string.stats_pages_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier
                    .weight(1f)
                    .testTag(StatsTestTags.PAGES),
            )
            OutlinedTextField(
                value = minutesInput,
                onValueChange = onMinutesChange,
                label = { Text(stringResource(R.string.stats_minutes_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier
                    .weight(1f)
                    .testTag(StatsTestTags.MINUTES),
            )
        }
        val locale = LocalLocale.current.platformLocale
        val dateText = remember(date, locale) {
            date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
        }
        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(
                    if (date == today) R.string.stats_date_today else R.string.stats_date_value,
                    dateText,
                ),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onCancel) { Text(stringResource(UiR.string.ui_cancel)) }
            Button(onClick = onSave, enabled = canSave) { Text(stringResource(UiR.string.ui_save)) }
        }
    }
    if (showDatePicker) {
        LogDatePicker(
            date = date,
            today = today,
            onConfirm = { picked ->
                onDateChange(picked)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookPicker(books: List<ShelfBook>, selectedWorkKey: String?, onBookSelected: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val noBook = stringResource(R.string.stats_no_particular_book)
    val selectedTitle = books.firstOrNull { it.book.workKey == selectedWorkKey }?.book?.title ?: noBook
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedTitle,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.stats_book_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            supportingText = if (books.isEmpty()) {
                { Text(stringResource(R.string.stats_no_reading_books)) }
            } else {
                null
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(noBook) },
                onClick = {
                    onBookSelected(null)
                    expanded = false
                },
            )
            books.forEach { book ->
                DropdownMenuItem(
                    text = { Text(book.book.title) },
                    leadingIcon = if (book.book.workKey == selectedWorkKey) {
                        { Icon(ShelfIcons.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        onBookSelected(book.book.workKey)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** The Material date picker works in UTC milliseconds; only today and earlier can be picked. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogDatePicker(date: LocalDate, today: LocalDate, onConfirm: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val todayMillis = today.toUtcMillis()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = date.toUtcMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayMillis

            override fun isSelectableYear(year: Int): Boolean = year <= today.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onConfirm(it.toUtcDate()) } ?: onDismiss()
            }) { Text(stringResource(UiR.string.ui_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(UiR.string.ui_cancel)) } },
    ) {
        DatePicker(state = state)
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
