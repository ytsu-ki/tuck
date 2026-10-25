package com.example.starbox.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.starbox.AppContainer
import com.example.starbox.capture.RecentNotifications
import com.example.starbox.data.db.MessageDao
import com.example.starbox.data.db.RuleDao
import com.example.starbox.data.model.CapturedMessage
import com.example.starbox.data.model.Rule
import com.example.starbox.rules.RuleEngine
import com.example.starbox.rules.RuleOrganizer
import com.example.starbox.rules.RuleStats
import com.example.starbox.rules.toMatchInput
import com.example.starbox.system.AppEntry
import com.example.starbox.system.InstalledApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class RuleDetailState(
    val rule: Rule,
    val appLabels: List<String>,
    val stats: RuleStats,
    /** Newest inbox messages this rule starred. */
    val recentMatches: List<CapturedMessage>,
    /** How many recently seen notifications the rule matches right now. */
    val previewMatches: Int,
    val recentCount: Int,
    /** Tags used by any rule, offered as suggestions. */
    val allTags: List<String>,
)

/** One rule's page. Every change is saved immediately; there is no separate Save step. */
class RuleDetailViewModel(
    private val ruleId: Long,
    private val ruleDao: RuleDao,
    messageDao: MessageDao,
    private val recent: RecentNotifications,
    private val apps: InstalledApps,
) : ViewModel() {

    private val _loaded = MutableStateFlow(false)

    /** True once the rule has been read at least once, so a later null means it was deleted. */
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    val state: StateFlow<RuleDetailState?> = combine(
        ruleDao.observeById(ruleId),
        ruleDao.observeAll(),
        messageDao.observeAll(),
        recent.items,
    ) { rule, allRules, messages, seen ->
        if (rule == null) return@combine null
        _loaded.value = true
        RuleDetailState(
            rule = rule,
            appLabels = rule.packages.map(apps::label),
            stats = RuleOrganizer.stats(messages)[rule.id] ?: RuleStats(),
            recentMatches = messages.filter { rule.id in it.matchedRuleIds }.sortedByDescending { it.postedAt }.take(5),
            previewMatches = seen.count { RuleEngine.matches(rule, it.toMatchInput()) },
            recentCount = seen.size,
            allTags = allRules.mapNotNull { it.tag?.trim()?.ifEmpty { null } }.distinct().sorted(),
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _installedApps = MutableStateFlow<List<AppEntry>?>(null)
    val installedApps: StateFlow<List<AppEntry>?> = _installedApps.asStateFlow()

    init {
        viewModelScope.launch { _installedApps.value = apps.launchable() }
    }

    private val writeLock = Mutex()

    /**
     * Applies [transform] to the latest saved rule. Reading fresh from the database under a lock
     * means quick successive edits (ticking several apps) can't overwrite each other.
     */
    fun update(transform: (Rule) -> Rule) {
        viewModelScope.launch {
            writeLock.withLock {
                val fresh = ruleDao.getById(ruleId) ?: return@withLock
                ruleDao.upsert(transform(fresh))
            }
        }
    }

    fun senderSuggestions(exclude: List<String>): List<String> {
        val rule = state.value?.rule ?: return emptyList()
        return RuleOrganizer.senderSuggestions(recent.items.value, rule.packages, exclude)
    }

    fun delete(onDone: () -> Unit) {
        val rule = state.value?.rule ?: return
        viewModelScope.launch {
            ruleDao.delete(rule)
            onDone()
        }
    }

    /** Copies the rule (paused, so it doesn't double-alert) and hands back the new ID. */
    fun duplicate(copySuffix: String, onCreated: (Long) -> Unit) {
        val rule = state.value?.rule ?: return
        viewModelScope.launch {
            val id = ruleDao.upsert(
                rule.copy(id = 0, name = rule.name + copySuffix, enabled = false, createdAt = System.currentTimeMillis()),
            )
            onCreated(id)
        }
    }

    companion object {
        fun factory(ruleId: Long, container: AppContainer) = viewModelFactory {
            initializer {
                RuleDetailViewModel(
                    ruleId = ruleId,
                    ruleDao = container.database.ruleDao(),
                    messageDao = container.database.messageDao(),
                    recent = container.recentNotifications,
                    apps = container.installedApps,
                )
            }
        }
    }
}
