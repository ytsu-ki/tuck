package com.example.starbox.ui.settings

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.starbox.R
import com.example.starbox.ui.theme.StarBoxTheme
import com.example.starbox.rules.QuietHours
import com.example.starbox.ui.components.CardDivider
import com.example.starbox.ui.components.Hint
import com.example.starbox.ui.components.SwitchRow
import java.util.Calendar

private enum class Editing { Start, End }

/** Quiet-hours rows for use inside a SectionCard; the times slide in when enabled. */
@Composable
fun QuietHoursSection(quietHours: QuietHours, onChange: (QuietHours) -> Unit) {
    val context = LocalContext.current
    var editing by rememberSaveable { mutableStateOf<Editing?>(null) }

    Column {
        SwitchRow(
            title = stringResource(R.string.quiet_enabled),
            subtitle = stringResource(R.string.quiet_desc),
            checked = quietHours.enabled,
            onCheckedChange = { onChange(quietHours.copy(enabled = it)) },
        )
        AnimatedVisibility(visible = quietHours.enabled) {
            Column {
                CardDivider()
                TimeRow(stringResource(R.string.quiet_start), formatMinute(context, quietHours.startMinute)) { editing = Editing.Start }
                CardDivider()
                TimeRow(stringResource(R.string.quiet_end), formatMinute(context, quietHours.endMinute)) { editing = Editing.End }
                Hint(stringResource(R.string.quiet_bypass_hint), Modifier.padding(bottom = 12.dp))
            }
        }
    }

    editing?.let { which ->
        TimePickerDialog(
            initialMinute = if (which == Editing.Start) quietHours.startMinute else quietHours.endMinute,
            onDismiss = { editing = null },
            onConfirm = { minute ->
                editing = null
                onChange(
                    if (which == Editing.Start) quietHours.copy(startMinute = minute) else quietHours.copy(endMinute = minute),
                )
            },
        )
    }
}

@Composable
private fun TimeRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(initialMinute: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initialMinute / 60,
        initialMinute = initialMinute % 60,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        containerColor = StarBoxTheme.floating,
        onDismissRequest = onDismiss,
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** Formats minutes-after-midnight in the user's 12/24-hour preference. */
private fun formatMinute(context: Context, minuteOfDay: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
        set(Calendar.MINUTE, minuteOfDay % 60)
    }
    return DateFormat.getTimeFormat(context).format(calendar.time)
}
