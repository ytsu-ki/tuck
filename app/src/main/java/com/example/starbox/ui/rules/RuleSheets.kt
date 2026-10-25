package com.example.starbox.ui.rules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.starbox.R
import com.example.starbox.ui.theme.StarBoxTheme
import com.example.starbox.data.model.Rule
import com.example.starbox.ui.components.AlertProfileEditor
import com.example.starbox.ui.components.CardDivider
import com.example.starbox.ui.components.ChipInputField
import com.example.starbox.ui.components.Hint
import com.example.starbox.ui.components.SoftFilterChip
import com.example.starbox.ui.components.SwitchRow
import com.example.starbox.ui.components.splitChipInput

/**
 * Edits one list (senders or keywords) on its own. Changes are kept when the sheet is closed
 * by Done or by swiping it away, including text typed but not yet confirmed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValuesSheet(
    title: String,
    description: String,
    label: String,
    placeholder: String,
    initial: List<String>,
    suggestions: (current: List<String>) -> List<String>,
    onDone: (List<String>) -> Unit,
) {
    var values by rememberSaveable { mutableStateOf(initial) }
    var input by rememberSaveable { mutableStateOf("") }
    fun finish() = onDone((values + splitChipInput(input)).map { it.trim() }.filter { it.isNotEmpty() }.distinct())

    ModalBottomSheet(
        onDismissRequest = ::finish,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = StarBoxTheme.floating,
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Hint(description, Modifier.padding(top = 4.dp, bottom = 16.dp))
            ChipInputField(
                label = label,
                placeholder = placeholder,
                values = values,
                input = input,
                onInputChange = { input = it },
                onValuesChange = { values = it },
            )
            SuggestionChips(suggestions(values), onPick = { values = values + it })
            Spacer(Modifier.height(20.dp))
            Button(onClick = ::finish, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text(stringResource(R.string.action_done))
            }
        }
    }
}

/** "Seen recently" names to tap instead of typing them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuggestionChips(suggestions: List<String>, onPick: (String) -> Unit) {
    AnimatedVisibility(visible = suggestions.isNotEmpty()) {
        Column(Modifier.padding(top = 16.dp)) {
            Text(
                stringResource(R.string.rule_suggestions_recent),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                suggestions.forEach { name ->
                    AssistChip(
                        onClick = { onPick(name) },
                        label = { Text(name) },
                        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        shape = CircleShape,
                        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = Color.Transparent),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    )
                }
            }
        }
    }
}

/** The rule's alert on its own sheet; each switch is saved as it changes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertSheet(rule: Rule, onChange: ((Rule) -> Rule) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = StarBoxTheme.floating,
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
            Text(stringResource(R.string.rule_alert_label), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            SwitchRow(
                title = stringResource(R.string.rule_alert_use_default),
                subtitle = stringResource(R.string.rule_alert_use_default_desc),
                checked = rule.useDefaultAlert,
                onCheckedChange = { on -> onChange { it.copy(useDefaultAlert = on) } },
            )
            AnimatedVisibility(visible = !rule.useDefaultAlert) {
                Column {
                    CardDivider()
                    AlertProfileEditor(profile = rule.alert, onChange = { profile -> onChange { it.copy(alert = profile) } })
                }
            }
            CardDivider()
            SwitchRow(
                title = stringResource(R.string.rule_bypass_quiet),
                subtitle = stringResource(R.string.rule_bypass_quiet_desc),
                checked = rule.bypassQuietHours,
                onCheckedChange = { on -> onChange { it.copy(bypassQuietHours = on) } },
            )
        }
    }
}

@Composable
fun RenameDialog(current: String, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        containerColor = StarBoxTheme.floating,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rule_rename)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onRename(name.trim()) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Pick an existing tag or type a new one; an empty tag removes it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagDialog(current: String?, existing: List<String>, onDismiss: () -> Unit, onSave: (String?) -> Unit) {
    var tag by rememberSaveable { mutableStateOf(current.orEmpty()) }
    AlertDialog(
        containerColor = StarBoxTheme.floating,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rule_tag_label)) },
        text = {
            Column {
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    placeholder = { Text(stringResource(R.string.rule_tag_placeholder)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (existing.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        existing.forEach { option ->
                            SoftFilterChip(selected = tag.trim() == option, onClick = { tag = option }, label = "#$option")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(tag.trim().ifEmpty { null }) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (current != null) {
                    TextButton(onClick = { onSave(null) }) { Text(stringResource(R.string.action_remove)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}
