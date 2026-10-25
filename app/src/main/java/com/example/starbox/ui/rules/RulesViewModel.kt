package com.example.starbox.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.starbox.AppContainer
import com.example.starbox.data.db.MessageDao
import com.example.starbox.data.db.RuleDao
import com.example.starbox.data.model.Rule
import com.example.starbox.rules.RuleGroupKey
import com.example.starbox.rules.RuleOrganizer
import com.example.starbox.rules.RuleStats
import com.example.starbox.system.InstalledApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RuleListItem(val rule: Rule, val appLabels: List<String>, val stats: RuleStats)

/** [appLabel] is set for single-app groups; the other two kinds get their titles in the UI. */
data class RuleListGroup(val key: RuleGroupKey, val appLabel: String?, val items: List<RuleListItem>)

data class RulesUiState(val groups: List<RuleListGroup>, val activeCount: Int, val pausedCount: Int)

class RulesViewModel(
    private val ruleDao: RuleDao,
    messageDao: MessageDao,
    private val apps: InstalledApps,
) : ViewModel() {

    /** Null until the first load, so the empty state doesn't flash. */
    val state: StateFlow<RulesUiState?> = combine(ruleDao.observeAll(), messageDao.observeAll()) { rules, messages ->
        val stats = RuleOrganizer.stats(messages)
        RulesUiState(
            groups = RuleOrganizer.group(rules, apps::label).map { group ->
                RuleListGroup(
                    key = group.key,
                    appLabel = (group.key as? RuleGroupKey.App)?.let { apps.label(it.packageName) },
                    items = group.rules.map { RuleListItem(it, it.packages.map(apps::label), stats[it.id] ?: RuleStats()) },
                )
            },
            activeCount = rules.count { it.enabled },
            pausedCount = rules.count { !it.enabled },
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setEnabled(rule: Rule, enabled: Boolean) {
        viewModelScope.launch { ruleDao.upsert(rule.copy(enabled = enabled)) }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { RulesViewModel(container.database.ruleDao(), container.database.messageDao(), container.installedApps) }
        }
    }
}
