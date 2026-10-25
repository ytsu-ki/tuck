package com.example.starbox.ui.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.starbox.LocalAppContainer
import com.example.starbox.R
import com.example.starbox.alerts.AlertHealth
import com.example.starbox.system.PermissionState
import com.example.starbox.system.Permissions
import com.example.starbox.ui.components.CardDivider
import com.example.starbox.ui.components.Hint
import com.example.starbox.ui.components.Pill

/** The three permission steps as divided rows, for use inside a SectionCard (onboarding and Settings). */
@Composable
fun PermissionList(state: PermissionState, alertHealth: AlertHealth? = null) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    // "Allowed" means alerts will really sound and pop up, not just that notifications are on.
    val alertsMuted = state.postNotifications && alertHealth?.settingsProblems?.isNotEmpty() == true
    // First tap asks via the system dialog; after that Android may stop showing it, so later taps open settings.
    var askedPostPermission by rememberSaveable { mutableStateOf(false) }
    val postPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { askedPostPermission = true }

    Column {
        PermissionRow(
            title = stringResource(R.string.perm_access_title),
            description = stringResource(R.string.perm_access_desc),
            required = true,
            granted = state.notificationAccess,
            onGrant = { Permissions.openNotificationAccessSettings(context) },
            footer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                {
                    Hint(stringResource(R.string.perm_access_restricted_hint), Modifier.padding(top = 8.dp))
                    TextButton(onClick = { Permissions.openAppDetails(context) }) {
                        Text(stringResource(R.string.perm_open_app_info))
                    }
                }
            } else {
                null
            },
        )
        CardDivider()
        PermissionRow(
            title = stringResource(R.string.perm_post_title),
            description = stringResource(if (alertsMuted) R.string.perm_post_desc_muted else R.string.perm_post_desc),
            required = false,
            granted = state.postNotifications && !alertsMuted,
            grantLabel = if (alertsMuted) stringResource(R.string.action_fix) else null,
            onGrant = {
                if (alertsMuted) {
                    Permissions.openFirst(context, container.alertNotifier.settingsIntents(container.prefs.defaultAlert.value))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !askedPostPermission) {
                    postPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    Permissions.openAppNotificationSettings(context)
                }
            },
        )
        CardDivider()
        PermissionRow(
            title = stringResource(R.string.perm_battery_title),
            description = stringResource(R.string.perm_battery_desc),
            required = false,
            granted = state.batteryUnrestricted,
            onGrant = { Permissions.requestBatteryUnrestricted(context) },
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    required: Boolean,
    granted: Boolean,
    onGrant: () -> Unit,
    grantLabel: String? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    Column(Modifier.padding(vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    if (required && !granted) {
                        Spacer(Modifier.width(8.dp))
                        Pill(
                            stringResource(R.string.perm_required),
                            container = MaterialTheme.colorScheme.primaryContainer,
                            content = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Hint(description)
            }
            Spacer(Modifier.width(12.dp))
            if (granted) {
                Pill(
                    stringResource(R.string.perm_granted),
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                    leading = { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp)) },
                )
            } else {
                FilledTonalButton(onClick = onGrant) { Text(grantLabel ?: stringResource(R.string.perm_grant)) }
            }
        }
        if (footer != null) {
            AnimatedVisibility(visible = !granted) { Column { footer() } }
        }
    }
}
