package com.example.starbox.ui.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.starbox.system.PermissionState
import com.example.starbox.system.Permissions

/** Current permission state, re-read every time the user returns from a settings screen. */
@Composable
fun rememberPermissionState(): PermissionState {
    val context = LocalContext.current
    var state by remember { mutableStateOf(Permissions.current(context)) }
    LifecycleResumeEffect(Unit) {
        state = Permissions.current(context)
        onPauseOrDispose { }
    }
    return state
}
