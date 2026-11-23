package com.example.shelf.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.GoalType
import java.time.LocalTime
import com.example.shelf.core.ui.R as UiR

/** A single-choice dialog: radio rows, applies the choice immediately. */
@Composable
internal fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = option == selected,
                                role = Role.RadioButton,
                                onClick = {
                                    onSelect(option)
                                    onDismiss()
                                },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Text(
                            text = label(option),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(UiR.string.ui_cancel)) }
        },
    )
}

/** Signing out keeps the library unless the user ticks the box (the safe choice is the default). */
@Composable
internal fun SignOutDialog(onConfirm: (clearLocalData: Boolean) -> Unit, onDismiss: () -> Unit) {
    var clearLocalData by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_sign_out_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.settings_sign_out_message))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .toggleable(value = clearLocalData, role = Role.Checkbox) { clearLocalData = it }
                        .testTag(SettingsTestTags.CLEAR_LOCAL_DATA),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Checkbox(checked = clearLocalData, onCheckedChange = null)
                    Text(stringResource(R.string.settings_sign_out_clear))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(clearLocalData)
                    onDismiss()
                },
            ) { Text(stringResource(R.string.settings_sign_out)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(UiR.string.ui_cancel)) } },
    )
}

@Composable
internal fun GoalDialog(goal: DailyGoal, onSave: (DailyGoal) -> Unit, onDismiss: () -> Unit) {
    var type by rememberSaveable { mutableStateOf(goal.type) }
    var target by rememberSaveable { mutableStateOf(goal.target.toString()) }
    val parsed = target.toIntOrNull()?.takeIf { it in DailyGoal.TARGET_RANGE }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_daily_goal)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    GoalType.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = option == type,
                            onClick = { type = option },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = GoalType.entries.size),
                        ) {
                            Text(
                                stringResource(
                                    if (option ==
                                        GoalType.PAGES
                                    ) {
                                        R.string.settings_goal_type_pages
                                    } else {
                                        R.string.settings_goal_type_minutes
                                    },
                                ),
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = target,
                    onValueChange = { text -> target = text.filter(Char::isDigit).take(MAX_TARGET_DIGITS) },
                    label = {
                        Text(
                            stringResource(
                                if (type ==
                                    GoalType.PAGES
                                ) {
                                    R.string.settings_goal_target_pages
                                } else {
                                    R.string.settings_goal_target_minutes
                                },
                            ),
                        )
                    },
                    isError = parsed == null,
                    supportingText = {
                        Text(
                            stringResource(
                                R.string.settings_goal_range,
                                DailyGoal.TARGET_RANGE.first,
                                DailyGoal.TARGET_RANGE.last,
                            ),
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(SettingsTestTags.GOAL_TARGET),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = parsed != null,
                onClick = {
                    parsed?.let { onSave(DailyGoal(type, it)) }
                    onDismiss()
                },
            ) { Text(stringResource(UiR.string.ui_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(UiR.string.ui_cancel)) }
        },
    )
}

@Composable
internal fun ReminderTimeDialog(
    time: LocalTime,
    is24Hour: Boolean,
    onSave: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = is24Hour)
    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_reminder_time)) },
        confirmButton = {
            TextButton(onClick = {
                onSave(LocalTime.of(state.hour, state.minute))
                onDismiss()
            }) { Text(stringResource(UiR.string.ui_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(UiR.string.ui_cancel)) }
        },
    ) {
        TimePicker(state = state)
    }
}

private const val MAX_TARGET_DIGITS = 4
