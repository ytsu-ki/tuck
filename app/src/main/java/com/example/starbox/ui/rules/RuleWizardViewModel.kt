package com.example.starbox.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.starbox.AppContainer
import com.example.starbox.capture.ParsedNotification
import com.example.starbox.capture.RecentNotifications
import com.example.starbox.data.db.RuleDao
import com.example.starbox.data.model.AlertProfile
import com.example.starbox.data.model.Rule
import com.example.starbox.rules.RuleEngine
import com.example.starbox.rules.RuleOrganizer
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The new-rule flow asks one question per page. */
enum class WizardStep { Apps, Senders, Keywords, Alert, Review }

class RuleWizardViewModel(
    private val ruleDao: RuleDao,
    private val recent: RecentNotifications,
    private val apps: InstalledApps,
    val defaultAlert: StateFlow<AlertProfile>,
) : ViewModel() {

    private val _draft = MutableStateFlow(Rule(name = ""))
    val draft: StateFlow<Rule> = _draft.asStateFlow()

    private val _step = MutableStateFlow(WizardStep.Apps)
    val step: StateFlow<WizardStep> = _step.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppEntry>?>(null)
    val installedApps: StateFlow<List<AppEntry>?> = _installedApps.asStateFlow()

    val recentItems: StateFlow<List<ParsedNotification>> = recent.items

    /** Recently seen notifications the draft would star, for the review page. */
    val preview: StateFlow<List<ParsedNotification>> =
        combine(_draft, recent.items) { rule, items -> items.filter { RuleEngine.matches(rule, it.toMatchInput()) } }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { _installedApps.value = apps.launchable() }
    }

    fun update(transform: (Rule) -> Rule) = _draft.update(transform)

    fun senderSuggestions(exclude: List<String>): List<String> =
        RuleOrganizer.senderSuggestions(recent.items.value, _draft.value.packages, exclude)

    fun appLabel(packageName: String) = apps.label(packageName)

    /** Moves forward; returns false (and stays) if leaving the last condition page with no conditions. */
    fun next(): Boolean {
        val current = _step.value
        if (current == WizardStep.Keywords && !RuleEngine.hasConditions(_draft.value)) return false
        WizardStep.entries.getOrNull(current.ordinal + 1)?.let { _step.value = it }
        return true
    }

    /** Returns false when already on the first page (the caller then closes the flow). */
    fun back(): Boolean {
        val previous = WizardStep.entries.getOrNull(_step.value.ordinal - 1) ?: return false
        _step.value = previous
        return true
    }

    /** The most specific condition makes the best default name: "Alice", "urgent" or "WeChat". */
    fun suggestedName(fallback: String): String {
        val rule = _draft.value
        return rule.senders.firstOrNull()
            ?: rule.keywords.firstOrNull()
            ?: rule.packages.firstOrNull()?.let(apps::label)
            ?: fallback
    }

    fun create(fallbackName: String, onCreated: (Long) -> Unit) {
        val rule = _draft.value
        if (!RuleEngine.hasConditions(rule)) return
        val named = rule.copy(
            name = rule.name.trim().ifEmpty { suggestedName(fallbackName) },
            tag = rule.tag?.trim()?.ifEmpty { null },
            createdAt = System.currentTimeMillis(),
        )
        viewModelScope.launch { onCreated(ruleDao.upsert(named)) }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                RuleWizardViewModel(
                    ruleDao = container.database.ruleDao(),
                    recent = container.recentNotifications,
                    apps = container.installedApps,
                    defaultAlert = container.prefs.defaultAlert,
                )
            }
        }
    }
}
