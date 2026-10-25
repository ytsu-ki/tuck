package com.example.starbox

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starbox.ui.StarBoxRoot
import com.example.starbox.ui.theme.CleanNeutrals
import com.example.starbox.ui.theme.StarBoxTheme
import kotlinx.coroutines.flow.MutableStateFlow

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }

class MainActivity : ComponentActivity() {

    /** Message to show after the user taps a StarBox alert; cleared once the inbox opens it. */
    private val openMessageRequest = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        val container = (application as StarBoxApp).container
        setContent {
            val openMessageId by openMessageRequest.collectAsStateWithLifecycle()
            val accent by container.prefs.accent.collectAsStateWithLifecycle()
            val themeMode by container.prefs.themeMode.collectAsStateWithLifecycle()
            val darkTheme = themeMode.isDark(isSystemInDarkTheme())

            // Keep status/navigation bar icons and the window behind Compose in step with the
            // chosen mode, which can differ from the phone's.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_NAV_SCRIM, DARK_NAV_SCRIM) { darkTheme },
                )
                window.setBackgroundDrawable(CleanNeutrals.of(darkTheme).background.toDrawable())
                onDispose {}
            }

            CompositionLocalProvider(LocalAppContainer provides container) {
                StarBoxTheme(accent = accent, darkTheme = darkTheme) {
                    StarBoxRoot(
                        openMessageId = openMessageId,
                        onOpenMessageHandled = { openMessageRequest.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val id = intent?.getLongExtra(EXTRA_MESSAGE_ID, -1L) ?: -1L
        if (id > 0) openMessageRequest.value = id
    }

    companion object {
        const val EXTRA_MESSAGE_ID = "open_message_id"

        // The same scrims enableEdgeToEdge() uses by default for 3-button navigation.
        private val LIGHT_NAV_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        private val DARK_NAV_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
