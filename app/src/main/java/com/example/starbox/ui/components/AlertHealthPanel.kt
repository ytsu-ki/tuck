package com.example.starbox.ui.components

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.starbox.LocalAppContainer
import com.example.starbox.R
import com.example.starbox.alerts.AlertHealth
import com.example.starbox.alerts.AlertProblem
import com.example.starbox.data.model.AlertProfile
import com.example.starbox.system.OemBattery
import com.example.starbox.system.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Alert health for [profile], re-checked on resume (back from system settings) and on [refresh]. */
class AlertHealthState(val health: AlertHealth?, val refresh: () -> Unit)

@Composable
fun rememberAlertHealth(profile: AlertProfile): AlertHealthState {
    val notifier = LocalAppContainer.current.alertNotifier
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        tick++
        onPauseOrDispose { }
    }
    val health by produceState<AlertHealth?>(null, profile, tick) {
        value = withContext(Dispatchers.IO) { notifier.health(profile) }
    }
    return AlertHealthState(health) { tick++ }
}

/**
 * "Will my alerts actually ring?" in one glance: a green line when everything's on, otherwise
 * each problem in plain words with the button that fixes it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlertHealthPanel(
    profile: AlertProfile,
    health: AlertHealth?,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    if (health == null) return

    AnimatedContent(targetState = health, label = "alertHealth", modifier = modifier) { current ->
        if (current.ok) {
            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(if (profile.isSilent) R.string.alert_health_silent_profile else R.string.alert_health_ok),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.alert_health_title_problem), style = MaterialTheme.typography.titleSmall)
                    }
                    current.problems.forEach { problem ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.padding(top = 2.dp).size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(problem.label()), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (current.settingsProblems.isNotEmpty()) {
                        OemBattery.current().alertHint?.let { hint ->
                            Text(stringResource(hint), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
                        if (current.settingsProblems.isNotEmpty()) {
                            Button(onClick = { Permissions.openFirst(context, container.alertNotifier.settingsIntents(profile)) }) {
                                Text(stringResource(R.string.alert_fix_in_settings))
                            }
                        }
                        if (AlertProblem.DoNotDisturb in current.phoneProblems) {
                            FilledTonalButton(onClick = { Permissions.openDoNotDisturbSettings(context) }) {
                                Text(stringResource(R.string.alert_open_dnd_settings))
                            }
                        }
                        if (current.phoneProblems.any { it != AlertProblem.DoNotDisturb }) {
                            FilledTonalButton(onClick = { Permissions.openSoundSettings(context) }) {
                                Text(stringResource(R.string.alert_open_sound_settings))
                            }
                        }
                        // Only a muted channel (not "notifications off") can be cured by a fresh one.
                        if (current.settingsProblems.any { it != AlertProblem.NotificationsOff }) {
                            TextButton(onClick = {
                                container.appScope.launch {
                                    container.alertNotifier.resetChannels()
                                    withContext(Dispatchers.Main) { onChanged() }
                                }
                            }) { Text(stringResource(R.string.alert_reset_channel)) }
                        }
                    }
                    if (current.settingsProblems.any { it != AlertProblem.NotificationsOff }) {
                        Text(stringResource(R.string.alert_reset_hint), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@StringRes
fun AlertProblem.label(): Int = when (this) {
    AlertProblem.NotificationsOff -> R.string.alert_problem_notifications_off
    AlertProblem.ChannelBlocked -> R.string.alert_problem_channel_blocked
    AlertProblem.BannerOff -> R.string.alert_problem_banner_off
    AlertProblem.SoundOff -> R.string.alert_problem_sound_off
    AlertProblem.VibrationOff -> R.string.alert_problem_vibration_off
    AlertProblem.DoNotDisturb -> R.string.alert_problem_dnd
    AlertProblem.PhoneSilent -> R.string.alert_problem_silent
    AlertProblem.PhoneVibrateOnly -> R.string.alert_problem_vibrate_only
    AlertProblem.VolumeZero -> R.string.alert_problem_volume_zero
}

/**
 * The alert check plus "Send test alert". After the test, the result is re-checked so a silent
 * test explains itself instead of leaving the user guessing.
 */
@Composable
fun AlertCheck(profile: AlertProfile, state: AlertHealthState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val sentOk = stringResource(R.string.alert_test_sent)
    val sentWithProblems = stringResource(R.string.alert_test_sent_problems)
    Column(modifier) {
        AlertHealthPanel(profile = profile, health = state.health, onChanged = state.refresh)
        if (!profile.isSilent) {
            FilledTonalButton(
                onClick = {
                    container.appScope.launch {
                        container.alertNotifier.sendTest(profile)
                        val after = container.alertNotifier.health(profile)
                        withContext(Dispatchers.Main) {
                            state.refresh()
                            Toast.makeText(context, if (after.ok) sentOk else sentWithProblems, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.padding(vertical = 8.dp),
            ) { Text(stringResource(R.string.alert_send_test)) }
        }
    }
}
