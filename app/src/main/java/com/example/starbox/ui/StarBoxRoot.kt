package com.example.starbox.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.starbox.LocalAppContainer
import com.example.starbox.capture.StarNotificationListener
import kotlinx.coroutines.delay
import com.example.starbox.ui.main.MainScaffold
import com.example.starbox.ui.onboarding.OnboardingScreen
import com.example.starbox.ui.permissions.rememberPermissionState

@Composable
fun StarBoxRoot(openMessageId: Long?, onOpenMessageHandled: () -> Unit) {
    val container = LocalAppContainer.current
    val permissions = rememberPermissionState()
    var onboardingDone by remember { mutableStateOf(container.prefs.onboardingDone) }
    val context = LocalContext.current

    // Some phones unbind the listener after updates or aggressive cleaning; nudge Android to
    // reconnect it whenever the app is opened with access granted but no live connection.
    LaunchedEffect(permissions.notificationAccess) {
        if (!permissions.notificationAccess) return@LaunchedEffect
        delay(3_000)
        if (!container.listenerConnected.value) {
            StarNotificationListener.requestRebind(context)
        }
    }

    if (!onboardingDone) {
        OnboardingScreen(
            permissions = permissions,
            onFinish = {
                container.prefs.onboardingDone = true
                onboardingDone = true
            },
        )
    } else {
        MainScaffold(
            permissions = permissions,
            openMessageId = openMessageId,
            onOpenMessageHandled = onOpenMessageHandled,
        )
    }
}
