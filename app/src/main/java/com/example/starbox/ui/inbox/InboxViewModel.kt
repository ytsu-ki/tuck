package com.example.starbox.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.starbox.AppContainer
import com.example.starbox.alerts.AlertNotifier
import com.example.starbox.data.db.MessageDao
import com.example.starbox.data.db.RuleDao
import com.example.starbox.data.model.CapturedMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Active hides messages already marked handled. */
enum class InboxView { Active, All }

data class InboxFilter(
    val view: InboxView = InboxView.Active,
    val packageName: String? = null,
    val tag: String? = null,
)

data class AppFilterOption(val packageName: String, val label: String)

data class InboxUiState(
    /** Every message, for looking up the one opened in the detail sheet. */
    val all: List<CapturedMessage>,
    val visible: List<CapturedMessage>,
    val filter: InboxFilter,
    val apps: List<AppFilterOption>,
    val tags: List<String>,
    val unreadCount: Int,
    /** Rule names by ID, for the "★ rule" pill on messages without tags. */
    val ruleNames: Map<Long, String>,
)

class InboxViewModel(
    private val messageDao: MessageDao,
    ruleDao: RuleDao,
    private val alertNotifier: AlertNotifier,
) : ViewModel() {

    private val filter = MutableStateFlow(InboxFilter())

    /** Null until the first load, so the empty state doesn't flash. */
    val state: StateFlow<InboxUiState?> = combine(messageDao.observeAll(), ruleDao.observeAll(), filter) { all, rules, f ->
        val apps = all.distinctBy { it.packageName }.map { AppFilterOption(it.packageName, it.appLabel) }.sortedBy { it.label }
        val tags = all.flatMap { it.tags }.distinct().sorted()
        // Drop a filter whose last message was deleted, so the list can't get stuck empty.
        val effective = f.copy(
            packageName = f.packageName?.takeIf { pkg -> apps.any { it.packageName == pkg } },
            tag = f.tag?.takeIf { it in tags },
        )
        InboxUiState(
            all = all,
            visible = all.filter { m ->
                (effective.view == InboxView.All || !m.isHandled) &&
                    (effective.packageName == null || m.packageName == effective.packageName) &&
                    (effective.tag == null || effective.tag in m.tags)
            },
            filter = effective,
            apps = apps,
            tags = tags,
            unreadCount = all.count { !it.isRead },
            ruleNames = rules.associate { it.id to it.name },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setView(view: InboxView) = filter.update { it.copy(view = view) }

    fun setApp(packageName: String?) = filter.update { it.copy(packageName = packageName) }

    fun setTag(tag: String?) = filter.update { it.copy(tag = tag) }

    fun markRead(message: CapturedMessage) {
        if (message.isRead) return
        viewModelScope.launch {
            messageDao.markRead(message.id)
            alertNotifier.cancel(message.id)
        }
    }

    fun setHandled(message: CapturedMessage, handled: Boolean) {
        viewModelScope.launch {
            messageDao.setHandled(message.id, handled)
            if (handled) alertNotifier.cancel(message.id)
        }
    }

    fun delete(message: CapturedMessage) {
        viewModelScope.launch {
            messageDao.deleteById(message.id)
            alertNotifier.cancel(message.id)
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            messageDao.markAllRead()
            alertNotifier.cancelAll()
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                InboxViewModel(container.database.messageDao(), container.database.ruleDao(), container.alertNotifier)
            }
        }
    }
}
