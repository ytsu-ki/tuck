package com.example.starbox.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.starbox.StarBoxApp
import kotlinx.coroutines.launch

/** Handles the "Mark handled" button on StarBox alerts without opening the app. */
class AlertActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_HANDLED) return
        val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1L)
        if (messageId < 0) return

        val container = (context.applicationContext as StarBoxApp).container
        val pending = goAsync()
        container.appScope.launch {
            try {
                container.database.messageDao().setHandled(messageId, handled = true)
                container.alertNotifier.cancel(messageId)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_MARK_HANDLED = "com.example.starbox.action.MARK_HANDLED"
        const val EXTRA_MESSAGE_ID = "message_id"
    }
}
