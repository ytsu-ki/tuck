package com.example.starbox.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.starbox.R

private val SEPARATORS = Regex("""[,，、;；\n]""")

/** Splits typed text on commas (ASCII or Chinese) and newlines into trimmed, non-empty values. */
fun splitChipInput(input: String): List<String> =
    input.split(SEPARATORS).map { it.trim() }.filter { it.isNotEmpty() }

/**
 * A text field that turns entries into removable chips. Typing a comma or pressing Enter adds
 * the entry. The pending [input] is hoisted so the caller can keep it when saving.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipInputField(
    label: String,
    placeholder: String,
    values: List<String>,
    input: String,
    onInputChange: (String) -> Unit,
    onValuesChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    fun commit(text: String) {
        val added = splitChipInput(text).filter { it !in values }
        if (added.isNotEmpty()) onValuesChange((values + added).distinct())
        onInputChange("")
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = input,
            onValueChange = { text ->
                if (SEPARATORS.containsMatchIn(text)) commit(text) else onInputChange(text)
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            supportingText = supportingText?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit(input) }),
            trailingIcon = {
                IconButton(onClick = { commit(input) }, enabled = input.isNotBlank()) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add))
                }
            },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        AnimatedVisibility(visible = values.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                values.forEach { value -> RemovablePill(value, onRemove = { onValuesChange(values - value) }) }
            }
        }
    }
}

/** A borderless pill with a small × that removes it. */
@Composable
fun RemovablePill(
    text: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    InputChip(
        selected = false,
        onClick = onRemove,
        label = { Text(text) },
        avatar = leading,
        trailingIcon = {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.action_remove),
                modifier = Modifier.size(InputChipDefaults.IconSize),
            )
        },
        shape = CircleShape,
        border = InputChipDefaults.inputChipBorder(
            enabled = true,
            selected = false,
            borderColor = Color.Transparent,
        ),
        colors = InputChipDefaults.inputChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier,
    )
}
