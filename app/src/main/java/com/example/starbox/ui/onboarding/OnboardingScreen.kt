package com.example.starbox.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starbox.LocalAppContainer
import com.example.starbox.R
import com.example.starbox.system.Oem
import com.example.starbox.system.OemBattery
import com.example.starbox.system.PermissionState
import com.example.starbox.ui.components.AlertCheck
import com.example.starbox.ui.components.CardDivider
import com.example.starbox.ui.components.Illustration
import com.example.starbox.ui.components.SectionCard
import com.example.starbox.ui.components.rememberAlertHealth
import com.example.starbox.ui.permissions.BackgroundGuidance
import com.example.starbox.ui.permissions.PermissionList
import com.example.starbox.ui.theme.StarBoxTheme
import com.example.starbox.ui.theme.muted

@Composable
fun OnboardingScreen(permissions: PermissionState, onFinish: () -> Unit) {
    val container = LocalAppContainer.current
    val defaultAlert by container.prefs.defaultAlert.collectAsStateWithLifecycle()
    val alertHealth = rememberAlertHealth(defaultAlert)
    Scaffold(
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                val buttonModifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                if (permissions.notificationAccess) {
                    Button(onClick = onFinish, shape = MaterialTheme.shapes.large, modifier = buttonModifier) {
                        Text(stringResource(R.string.onboarding_start))
                    }
                } else {
                    OutlinedButton(onClick = onFinish, shape = MaterialTheme.shapes.large, modifier = buttonModifier) {
                        Text(stringResource(R.string.onboarding_skip))
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(32.dp))
            Illustration(Icons.Filled.Star, size = 96.dp, tint = StarBoxTheme.star.star)
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            SectionCard {
                Promise(Icons.Filled.Notifications, stringResource(R.string.onboarding_promise_untouched))
                CardDivider()
                Promise(Icons.Filled.Lock, stringResource(R.string.onboarding_promise_local))
            }
            Spacer(Modifier.height(24.dp))
            SectionCard(title = stringResource(R.string.onboarding_setup)) {
                PermissionList(permissions, alertHealth.health)
            }
            // Once alerts are allowed, let people hear one before they rely on it.
            AnimatedVisibility(visible = permissions.postNotifications) {
                Column {
                    Spacer(Modifier.height(24.dp))
                    SectionCard(title = stringResource(R.string.onboarding_alert_check)) {
                        AlertCheck(profile = defaultAlert, state = alertHealth, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
            // Stock Android rarely kills the listener; brand skins often do, so warn up front there.
            if (remember { OemBattery.current() } != Oem.Other) {
                Spacer(Modifier.height(24.dp))
                SectionCard(title = stringResource(R.string.settings_section_background)) {
                    BackgroundGuidance(initiallyExpanded = true)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Promise(icon: ImageVector, text: String) {
    Row(Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.muted)
    }
}
