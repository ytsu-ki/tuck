package com.example.starbox.ui.settings

import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starbox.AppPrefs
import com.example.starbox.BuildConfig
import com.example.starbox.LocalAppContainer
import com.example.starbox.R
import com.example.starbox.capture.ParsedNotification
import com.example.starbox.rules.RuleEngine
import com.example.starbox.rules.toMatchInput
import com.example.starbox.system.NightMode
import com.example.starbox.system.PermissionState
import com.example.starbox.system.Permissions
import com.example.starbox.ui.components.AlertCheck
import com.example.starbox.ui.components.AlertProfileEditor
import com.example.starbox.ui.components.CardDivider
import com.example.starbox.ui.components.Dot
import com.example.starbox.ui.components.Hint
import com.example.starbox.ui.components.NotificationRow
import com.example.starbox.ui.components.SectionCard
import com.example.starbox.ui.components.SoftFilterChip
import com.example.starbox.ui.components.TabScreen
import com.example.starbox.ui.components.rememberAlertHealth
import com.example.starbox.ui.permissions.BackgroundGuidance
import com.example.starbox.ui.permissions.PermissionList
import com.example.starbox.ui.theme.muted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(contentPadding: PaddingValues, permissions: PermissionState) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val connected by container.listenerConnected.collectAsStateWithLifecycle()
    val recent by container.recentNotifications.items.collectAsStateWithLifecycle()
    val defaultAlert by container.prefs.defaultAlert.collectAsStateWithLifecycle()
    val quietHours by container.prefs.quietHours.collectAsStateWithLifecycle()
    val retentionDays by container.prefs.retentionDays.collectAsStateWithLifecycle()
    val accent by container.prefs.accent.collectAsStateWithLifecycle()
    val themeMode by container.prefs.themeMode.collectAsStateWithLifecycle()
    val alertHealth = rememberAlertHealth(defaultAlert)
    val rulesFlow = remember(container) { container.database.ruleDao().observeAll() }
    val rules by rulesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    // Which enabled rules star each recently seen notification, so rules can be checked live.
    val matchedNames = remember(recent, rules) {
        recent.associate { n ->
            n.dedupKey to RuleEngine.matchingRules(n.toMatchInput(), rules).map { it.name }
        }
    }

    TabScreen(title = stringResource(R.string.tab_settings), contentPadding = contentPadding) { inner ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = inner.calculateTopPadding() + 8.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item(key = "permissions") {
                SectionCard(title = stringResource(R.string.settings_section_permissions)) {
                    PermissionList(permissions, alertHealth.health)
                }
            }
            item(key = "alert") {
                SectionCard(title = stringResource(R.string.settings_section_alert)) {
                    Hint(stringResource(R.string.settings_alert_hint), Modifier.padding(top = 10.dp, bottom = 4.dp))
                    AlertProfileEditor(
                        profile = defaultAlert,
                        onChange = { profile ->
                            container.prefs.setDefaultAlert(profile)
                            // Create the matching channel now so system settings show it straight away.
                            container.appScope.launch {
                                container.alertNotifier.ensureChannels()
                                withContext(Dispatchers.Main) { alertHealth.refresh() }
                            }
                        },
                    )
                    CardDivider()
                    AlertCheck(profile = defaultAlert, state = alertHealth, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
            item(key = "background") {
                SectionCard(title = stringResource(R.string.settings_section_background)) {
                    BackgroundGuidance()
                }
            }
            item(key = "appearance") {
                SectionCard(title = stringResource(R.string.settings_section_appearance)) {
                    ThemeModePicker(
                        selected = themeMode,
                        onSelect = { mode ->
                            container.prefs.setThemeMode(mode)
                            NightMode.apply(context, mode)
                        },
                    )
                    CardDivider()
                    AccentPicker(selected = accent, onSelect = container.prefs::setAccent)
                }
            }
            item(key = "quiet") {
                SectionCard(title = stringResource(R.string.settings_section_quiet)) {
                    QuietHoursSection(quietHours, onChange = container.prefs::setQuietHours)
                }
            }
            item(key = "retention") {
                SectionCard(title = stringResource(R.string.settings_section_retention)) {
                    RetentionSection(
                        days = retentionDays,
                        onChange = { days ->
                            container.prefs.setRetentionDays(days)
                            container.appScope.launch { container.retentionCleaner.run() }
                        },
                    )
                }
            }
            item(key = "listener") {
                SectionCard(title = stringResource(R.string.settings_section_listener)) {
                    ListenerStatus(connected = connected, accessGranted = permissions.notificationAccess)
                    CardDivider()
                    RecentlySeen(
                        recent = recent,
                        matchedNames = matchedNames,
                        onClear = container.recentNotifications::clear,
                    )
                }
            }
            item(key = "about") {
                SectionCard(title = stringResource(R.string.settings_section_about)) {
                    Column(Modifier.padding(vertical = 12.dp)) {
                        Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.titleSmall)
                        Hint(stringResource(R.string.about_privacy), Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RetentionSection(days: Int, onChange: (Int) -> Unit) {
    Column(Modifier.padding(vertical = 12.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppPrefs.RETENTION_OPTIONS.forEach { option ->
                SoftFilterChip(
                    selected = days == option,
                    onClick = { onChange(option) },
                    label = when (option) {
                        0 -> stringResource(R.string.retention_forever)
                        365 -> stringResource(R.string.retention_one_year)
                        else -> pluralStringResource(R.plurals.retention_days, option, option)
                    },
                )
            }
        }
        Hint(
            stringResource(if (days == 0) R.string.retention_hint_forever else R.string.retention_hint),
            Modifier.padding(top = 10.dp),
        )
    }
}

private enum class Reconnect { Idle, Trying, Failed }

/**
 * Reader status with a Reconnect that reports back: a spinner while Android rebinds, a toast
 * once the reader runs again, and the manual fix when Android doesn't respond.
 */
@Composable
private fun ListenerStatus(connected: Boolean, accessGranted: Boolean) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var reconnect by remember { mutableStateOf(Reconnect.Idle) }
    val reconnectedMessage = stringResource(R.string.listener_reconnected)
    val readerError by container.prefs.readerError.collectAsStateWithLifecycle()

    // Confirms the fix whichever way it came: this button, or access switched off and on in settings.
    LaunchedEffect(connected) {
        if (connected && reconnect != Reconnect.Idle) {
            reconnect = Reconnect.Idle
            Toast.makeText(context, reconnectedMessage, Toast.LENGTH_SHORT).show()
        }
    }
    // With access off, the status line and the Permissions card already say what to do.
    LaunchedEffect(accessGranted) {
        if (!accessGranted) reconnect = Reconnect.Idle
    }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        ) {
            Dot(if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, size = 10.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(
                    when {
                        connected -> R.string.listener_connected
                        reconnect == Reconnect.Trying -> R.string.listener_reconnecting
                        accessGranted -> R.string.listener_waiting
                        else -> R.string.listener_no_access
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (accessGranted && !connected) {
                if (reconnect == Reconnect.Trying) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .padding(horizontal = 14.dp)
                            .size(20.dp),
                    )
                } else {
                    TextButton(onClick = {
                        reconnect = Reconnect.Trying
                        scope.launch {
                            if (!container.reconnectListener()) reconnect = Reconnect.Failed
                        }
                    }) {
                        Text(stringResource(if (reconnect == Reconnect.Failed) R.string.action_try_again else R.string.action_reconnect))
                    }
                }
            }
        }
        AnimatedVisibility(visible = accessGranted && !connected && reconnect == Reconnect.Failed) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.listener_reconnect_failed_title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.listener_reconnect_failed_body), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.listener_reconnect_failed_background), style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = { Permissions.openNotificationAccessSettings(context) },
                        modifier = Modifier.padding(top = 6.dp),
                    ) { Text(stringResource(R.string.action_open_notification_access)) }
                }
            }
        }
        // What went wrong last, in a form that can be passed on when asking for help.
        readerError?.let { error ->
            val time = DateUtils.formatDateTime(
                context,
                error.at,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
            )
            Hint(stringResource(R.string.listener_last_error, time, error.summary), Modifier.padding(bottom = 12.dp))
        }
    }
}

/** Collapsed by default: a diagnostic list most people only open when checking a rule. */
@Composable
private fun RecentlySeen(
    recent: List<ParsedNotification>,
    matchedNames: Map<String, List<String>>,
    onClear: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.settings_recent_title, recent.size),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Hint(stringResource(R.string.settings_recent_hint))
            }
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(if (expanded) R.string.action_collapse else R.string.action_expand),
                tint = MaterialTheme.colorScheme.muted,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                if (recent.isEmpty()) {
                    Hint(stringResource(R.string.settings_recent_empty), Modifier.padding(bottom = 14.dp))
                } else {
                    recent.forEach { notification ->
                        CardDivider()
                        NotificationRow(notification, matchedRuleNames = matchedNames[notification.dedupKey].orEmpty())
                    }
                    CardDivider()
                    TextButton(onClick = onClear, modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(stringResource(R.string.action_clear))
                    }
                }
            }
        }
    }
}
