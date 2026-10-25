package com.example.starbox

import android.app.Application
import kotlinx.coroutines.launch

class StarBoxApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appScope.launch { container.retentionCleaner.runIfDue() }
        // Alert channels exist (with sound, vibration and banners on) before the first alert.
        container.appScope.launch { container.alertNotifier.ensureChannels() }
    }
}
