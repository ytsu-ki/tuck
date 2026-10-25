package com.example.starbox.ui.inbox

import android.content.ClipData
import android.content.ClipboardManager
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.starbox.LocalAppContainer
import com.example.starbox.R
import com.example.starbox.ui.theme.StarBoxTheme
import com.example.starbox.data.model.CapturedMessage
import com.example.starbox.system.SourceLauncher
import com.example.starbox.ui.components.AppIcon
import com.example.starbox.ui.components.RulePill
import com.example.starbox.ui.components.TagPill
import com.example.starbox.ui.theme.muted

/** Full message plus every secondary action, so the inbox list itself stays uncluttered. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MessageDetailSheet(
    message: CapturedMessage,
    ruleNames: List<String>,
    onDismiss: () -> Unit,
    onToggleHandled: () -> Unit,
    onDelete: () -> Unit,
    onSaveAsMemo: () -> Unit,
) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val copiedText = stringResource(R.string.message_copied)
    val openFailedText = stringResource(R.string.message_open_failed)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = StarBoxTheme.floating,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Box(Modifier.padding(8.dp)) { AppIcon(message.packageName, size = 24.dp) }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(message.appLabel, style = MaterialTheme.typography.labelLarge)
                    Text(
                        DateUtils.formatDateTime(
                            context,
                            message.postedAt,
                            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.muted,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(message.sender ?: message.title ?: message.appLabel, style = MaterialTheme.typography.titleLarge)
            message.conversation?.let {
                Text(
                    stringResource(R.string.message_in_conversation, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.muted,
                )
            }
            Spacer(Modifier.height(12.dp))
            SelectionContainer {
                Text(message.text, style = MaterialTheme.typography.bodyLarge)
            }
            if (message.tags.isNotEmpty() || ruleNames.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    ruleNames.forEach { RulePill(it) }
                    message.tags.forEach { TagPill(it) }
                }
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    val opened = SourceLauncher.open(context, message.packageName, message.notificationKey, container.liveSourceIntents)
                    if (opened) onDismiss() else Toast.makeText(context, openFailedText, Toast.LENGTH_SHORT).show()
                },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.message_open_in, message.appLabel))
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                ActionTile(
                    painter = rememberVectorPainter(Icons.Filled.Create),
                    label = stringResource(R.string.message_action_memo),
                    onClick = onSaveAsMemo,
                    modifier = Modifier.weight(1f),
                )
                ActionTile(
                    painter = rememberVectorPainter(if (message.isHandled) Icons.Filled.CheckCircle else Icons.Filled.Done),
                    label = stringResource(if (message.isHandled) R.string.message_action_unhandle else R.string.message_action_handle),
                    onClick = onToggleHandled,
                    modifier = Modifier.weight(1f),
                )
                ActionTile(
                    painter = painterResource(R.drawable.ic_copy),
                    label = stringResource(R.string.message_copy),
                    onClick = {
                        context.getSystemService(ClipboardManager::class.java)
                            .setPrimaryClip(ClipData.newPlainText(message.appLabel, message.text))
                        Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                )
                ActionTile(
                    painter = rememberVectorPainter(Icons.Filled.Delete),
                    label = stringResource(R.string.action_delete),
                    onClick = { confirmDelete = true },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            containerColor = StarBoxTheme.floating,
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.message_delete_title)) },
            text = { Text(stringResource(R.string.message_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

/** Round tonal icon with a short label underneath. */
@Composable
private fun ActionTile(painter: Painter, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painter, contentDescription = null, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}
