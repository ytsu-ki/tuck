package com.example.starbox.ui.inbox

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.starbox.LocalAppContainer
import com.example.starbox.R
import com.example.starbox.data.model.CapturedMessage
import com.example.starbox.system.PermissionState
import com.example.starbox.ui.components.AppIcon
import com.example.starbox.ui.components.Dot
import com.example.starbox.ui.components.EmptyState
import com.example.starbox.ui.components.RulePill
import com.example.starbox.ui.components.SoftFilterChip
import com.example.starbox.ui.components.TabScreen
import com.example.starbox.ui.components.TagPill
import com.example.starbox.ui.components.rememberAlertHealth
import com.example.starbox.ui.theme.StarBoxTheme
import com.example.starbox.ui.theme.muted
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay

@Composable
fun InboxScreen(
    contentPadding: PaddingValues,
    permissions: PermissionState,
    openMessageId: Long?,
    onOpenMessageHandled: () -> Unit,
    onOpenSettings: () -> Unit,
    onSaveAsMemo: (messageId: Long) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: InboxViewModel = viewModel(factory = InboxViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val defaultAlert by container.prefs.defaultAlert.collectAsStateWithLifecycle()
    val alertHealth = rememberAlertHealth(defaultAlert)
    val readerConnected by container.listenerConnected.collectAsStateWithLifecycle()
    // Android takes a moment to connect the reader after Tuck starts; warn only if it doesn't.
    var readerLate by remember { mutableStateOf(false) }
    LaunchedEffect(readerConnected, permissions.notificationAccess) {
        readerLate = false
        if (permissions.notificationAccess && !readerConnected) {
            delay(READER_GRACE_MS)
            readerLate = true
        }
    }

    // Opening from a StarBox alert shows that message straight away.
    LaunchedEffect(openMessageId) {
        if (openMessageId != null) {
            selectedId = openMessageId
            onOpenMessageHandled()
        }
    }

    TabScreen(
        title = stringResource(R.string.tab_inbox),
        contentPadding = contentPadding,
        actions = {
            AnimatedVisibility(visible = (state?.unreadCount ?: 0) > 0, enter = fadeIn(), exit = fadeOut()) {
                IconButton(onClick = viewModel::markAllRead) {
                    Icon(Icons.Filled.Done, contentDescription = stringResource(R.string.inbox_mark_all_read))
                }
            }
        },
    ) { inner ->
        val current = state
        Column(Modifier.padding(inner)) {
            AnimatedVisibility(visible = !permissions.notificationAccess) {
                WarningBanner(stringResource(R.string.inbox_access_missing), onOpenSettings)
            }
            // Access on but nothing is being read: the inbox would quietly stay empty otherwise.
            AnimatedVisibility(visible = permissions.notificationAccess && !readerConnected && readerLate) {
                WarningBanner(stringResource(R.string.inbox_reader_stopped), onOpenSettings)
            }
            // Phone-wide modes (silent, Do Not Disturb) are the user's choice; only nag about
            // StarBox's own notification settings being muted.
            AnimatedVisibility(
                visible = permissions.notificationAccess && alertHealth.health?.settingsProblems?.isNotEmpty() == true,
            ) {
                WarningBanner(stringResource(R.string.inbox_alerts_muted), onOpenSettings)
            }
            when {
                current == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                current.all.isEmpty() -> EmptyState(
                    icon = Icons.Filled.Star,
                    title = stringResource(R.string.inbox_empty_title),
                    body = stringResource(R.string.inbox_empty_body),
                )
                else -> {
                    FilterBar(current, viewModel)
                    if (current.visible.isEmpty()) {
                        EmptyState(
                            icon = Icons.Filled.CheckCircle,
                            title = stringResource(R.string.inbox_filtered_empty_title),
                            body = stringResource(R.string.inbox_filtered_empty_body),
                        )
                    } else {
                        MessageList(
                            messages = current.visible,
                            ruleNames = current.ruleNames,
                            onOpen = { message ->
                                selectedId = message.id
                                viewModel.markRead(message)
                            },
                            onToggleHandled = { viewModel.setHandled(it, !it.isHandled) },
                        )
                    }
                }
            }
        }
    }

    val selected = state?.all?.firstOrNull { it.id == selectedId }
    if (selected != null) {
        LaunchedEffect(selected.id) { viewModel.markRead(selected) }
        MessageDetailSheet(
            message = selected,
            ruleNames = selected.matchedRuleIds.mapNotNull { state?.ruleNames?.get(it) },
            onDismiss = { selectedId = null },
            onToggleHandled = { viewModel.setHandled(selected, !selected.isHandled) },
            onDelete = {
                selectedId = null
                viewModel.delete(selected)
            },
            onSaveAsMemo = {
                selectedId = null
                onSaveAsMemo(selected.id)
            },
        )
    }
}

@Composable
private fun FilterBar(state: InboxUiState, viewModel: InboxViewModel) {
    val filter = state.filter
    LazyRow(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "view:active") {
            SoftFilterChip(
                selected = filter.view == InboxView.Active,
                onClick = { viewModel.setView(InboxView.Active) },
                label = stringResource(R.string.inbox_view_active),
            )
        }
        item(key = "view:all") {
            SoftFilterChip(
                selected = filter.view == InboxView.All,
                onClick = { viewModel.setView(InboxView.All) },
                label = stringResource(R.string.inbox_view_all),
            )
        }
        if (state.apps.size > 1) {
            item(key = "gap:apps") { Spacer(Modifier.width(4.dp)) }
            items(state.apps, key = { "app:" + it.packageName }) { app ->
                val selected = filter.packageName == app.packageName
                SoftFilterChip(
                    selected = selected,
                    onClick = { viewModel.setApp(if (selected) null else app.packageName) },
                    label = app.label,
                    leadingIcon = { AppIcon(app.packageName, size = 18.dp) },
                )
            }
        }
        if (state.tags.isNotEmpty()) {
            item(key = "gap:tags") { Spacer(Modifier.width(4.dp)) }
            items(state.tags, key = { "tag:$it" }) { tag ->
                val selected = filter.tag == tag
                SoftFilterChip(
                    selected = selected,
                    onClick = { viewModel.setTag(if (selected) null else tag) },
                    label = "#$tag",
                )
            }
        }
    }
}

@Composable
private fun MessageList(
    messages: List<CapturedMessage>,
    ruleNames: Map<Long, String>,
    onOpen: (CapturedMessage) -> Unit,
    onToggleHandled: (CapturedMessage) -> Unit,
) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val byDay = messages.groupBy { Instant.ofEpochMilli(it.postedAt).atZone(zone).toLocalDate() }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        byDay.forEach { (day, dayMessages) ->
            item(key = "day:$day") {
                Text(
                    dayLabel(context, day, dayMessages.first().postedAt),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.muted,
                    modifier = Modifier
                        .animateItem()
                        .padding(start = 4.dp, top = 12.dp, bottom = 2.dp),
                )
            }
            items(dayMessages, key = { it.id }) { message ->
                MessageCard(
                    message = message,
                    ruleName = message.matchedRuleIds.firstNotNullOfOrNull(ruleNames::get),
                    onClick = { onOpen(message) },
                    onToggleHandled = { onToggleHandled(message) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

private fun dayLabel(context: Context, day: LocalDate, sampleTime: Long): String {
    val today = LocalDate.now()
    return when (day) {
        today -> context.getString(R.string.day_today)
        today.minusDays(1) -> context.getString(R.string.day_yesterday)
        else -> DateUtils.formatDateTime(
            context,
            sampleTime,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL,
        )
    }
}

@Composable
private fun MessageCard(
    message: CapturedMessage,
    ruleName: String?,
    onClick: () -> Unit,
    onToggleHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unread = !message.isRead
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (message.isHandled) 0.55f else 1f),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                AppIcon(message.packageName, size = 20.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    listOfNotNull(message.sender ?: message.title, message.conversation).joinToString(" · ")
                        .ifEmpty { message.appLabel },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (unread) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    relativeTime(message.postedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.muted,
                    modifier = Modifier.padding(start = 12.dp),
                )
                AnimatedVisibility(visible = unread, enter = scaleIn(), exit = scaleOut()) {
                    Dot(StarBoxTheme.star.star, Modifier.padding(start = 8.dp))
                }
            }
            if (message.text.isNotEmpty()) {
                Text(
                    message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (unread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp, end = 8.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(44.dp)) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Tags say more than rule names, so they win; otherwise show what starred it.
                    if (message.tags.isNotEmpty()) {
                        message.tags.take(2).forEach { TagPill(it) }
                    } else if (ruleName != null) {
                        RulePill(ruleName)
                    }
                }
                IconButton(onClick = onToggleHandled, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (message.isHandled) Icons.Filled.CheckCircle else Icons.Filled.Done,
                        contentDescription = stringResource(
                            if (message.isHandled) R.string.inbox_mark_unhandled else R.string.inbox_mark_handled,
                        ),
                        tint = if (message.isHandled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.muted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

private fun relativeTime(time: Long): String =
    DateUtils.getRelativeTimeSpanString(
        time,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()

private const val READER_GRACE_MS = 8_000L

@Composable
private fun WarningBanner(text: String, onFix: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onFix) { Text(stringResource(R.string.action_fix)) }
        }
    }
}
