package com.example.starbox.system

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import com.example.starbox.ui.theme.ThemeMode

/**
 * Tells Android 12+ which mode StarBox uses, so the launch screen already has the right
 * background before the app draws. Older versions rely on the in-app theme alone.
 */
object NightMode {
    fun apply(context: Context, mode: ThemeMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val nightMode = when (mode) {
            ThemeMode.System -> UiModeManager.MODE_NIGHT_AUTO
            ThemeMode.Light -> UiModeManager.MODE_NIGHT_NO
            ThemeMode.Dark -> UiModeManager.MODE_NIGHT_YES
        }
        context.getSystemService(UiModeManager::class.java)?.setApplicationNightMode(nightMode)
    }
}
