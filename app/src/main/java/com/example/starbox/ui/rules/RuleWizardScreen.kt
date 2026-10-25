package com.example.starbox.ui.rules

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.starbox.LocalAppContainer
import com.example.starbox.R
import com.example.starbox.capture.ParsedNotification
import com.example.starbox.data.model.AlertProfile
import com.example.starbox.data.model.Rule
import com.example.starbox.rules.AlertPolicy
import com.example.starbox.ui.components.AlertProfileEditor
import com.example.starbox.ui.components.AppIcon
import com.example.starbox.ui.components.CardDivider
import com.example.starbox.ui.components.ChipInputField
import com.example.starbox.ui.components.Hint
import com.example.starbox.ui.components.NotificationRow
import com.example.starbox.ui.components.RemovablePill
import com.example.starbox.ui.components.SectionCard
import com.example.starbox.ui.components.SwitchRow
import com.example.starbox.ui.components.splitChipInput
import com.example.starbox.ui.theme.muted

/**
 * New rule, one question per page: which apps → from whom → which words → how to alert →
 * name and review. Sender and keyword pages can be skipped; the review shows what it would catch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleWizardScreen(onClose: () -> Unit, onCreated: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: RuleWizardViewModel = viewModel(factory = RuleWizardViewModel.factory(container))
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val step by viewModel.step.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val recentItems by viewModel.recentItems.collectAsStateWithLifecycle()
    val defaultAlert by viewModel.defaultAlert.collectAsStateWithLifecycle()

    var senderInput by rememberSaveable { mutableStateOf("") }
    var keywordInput by rememberSaveable { mutableStateOf("") }
    var showConditionError by rememberSaveable { mutableStateOf(false) }
    val fallbackName = stringResource(R.string.rule_default_name)

    // Typed-but-unconfirmed text counts, so "Next" never silently drops a name or keyword.
    fun commitPending() {
        if (senderInput.isNotBlank()) {
            val added = splitChipInput(senderInput)
            viewModel.update { it.copy(senders = (it.senders + added).distinct()) }
            senderInput = ""
        }
        if (keywordInput.isNotBlank()) {
            val added = splitChipInput(keywordInput)
            viewModel.update { it.copy(keywords = (it.keywords + added).distinct()) }
            keywordInput = ""
        }
    }

    fun goBack() {
        if (!viewModel.back()) onClose()
    }

    BackHandler(onBack = ::goBack)

    val progress by animateFloatAsState((step.ordinal + 1f) / WizardStep.entries.size, label = "wizardProgress")

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
                title = {
                    Column {
                        Text(stringResource(R.string.rule_new))
                        Text(
                            stringResource(R.string.wizard_step_of, step.ordinal + 1, WizardStep.entries.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.muted,
                        )
                    }
                },
            )
        },
        bottomBar = {
            WizardBar(
                showBack = step != WizardStep.Apps,
                primaryLabel = when {
                    step == WizardStep.Review -> stringResource(R.string.wizard_create)
                    step == WizardStep.Senders && draft.senders.isEmpty() && senderInput.isBlank() ->
                        stringResource(R.string.wizard_skip)
                    step == WizardStep.Keywords && draft.keywords.isEmpty() && keywordInput.isBlank() ->
                        stringResource(R.string.wizard_skip)
                    else -> stringResource(R.string.wizard_next)
                },
                onBack = ::goBack,
                onPrimary = {
                    commitPending()
                    if (step == WizardStep.Review) {
                        viewModel.create(fallbackName, onCreated)
                    } else {
                        showConditionError = !viewModel.next()
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                drawStopIndicator = {},
            )
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    (slideInHorizontally { if (forward) it / 4 else -it / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { if (forward) -it / 4 else it / 4 } + fadeOut())
                },
                label = "wizardStep",
                modifier = Modifier.weight(1f),
            ) { shown ->
                when (shown) {
                    WizardStep.Apps -> Page(R.string.wizard_apps_title, R.string.wizard_apps_body, scrollable = false) {
                        SelectedApps(draft, viewModel::appLabel) { pkg -> viewModel.update { it.copy(packages = it.packages - pkg) } }
                        AppPickerList(
                            apps = installedApps,
                            selected = draft.packages,
                            onToggle = { pkg ->
                                viewModel.update { r -> r.copy(packages = if (pkg in r.packages) r.packages - pkg else r.packages + pkg) }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 8.dp),
                        )
                    }
                    WizardStep.Senders -> Page(R.string.wizard_senders_title, R.string.wizard_senders_body) {
                        ChipInputField(
                            label = stringResource(R.string.rule_senders_label),
                            placeholder = stringResource(R.string.rule_senders_placeholder),
                            values = draft.senders,
                            input = senderInput,
                            onInputChange = { senderInput = it },
                            onValuesChange = { values -> viewModel.update { it.copy(senders = values) } },
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                        Column(Modifier.padding(horizontal = 20.dp)) {
                            SuggestionChips(
                                suggestions = viewModel.senderSuggestions(draft.senders),
                                onPick = { name -> viewModel.update { it.copy(senders = it.senders + name) } },
                            )
                        }
                    }
                    WizardStep.Keywords -> Page(R.string.wizard_keywords_title, R.string.wizard_keywords_body) {
                        ChipInputField(
                            label = stringResource(R.string.rule_keywords_label),
                            placeholder = stringResource(R.string.rule_keywords_placeholder),
                            values = draft.keywords,
                            input = keywordInput,
                            onInputChange = {
                                keywordInput = it
                                showConditionError = false
                            },
                            onValuesChange = { values -> viewModel.update { it.copy(keywords = values) } },
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                        AnimatedVisibility(visible = showConditionError) {
                            Text(
                                stringResource(R.string.rule_needs_condition),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            )
                        }
                    }
                    WizardStep.Alert -> Page(R.string.wizard_alert_title, R.string.wizard_alert_body) {
                        AlertChoices(draft, defaultAlert, onChange = viewModel::update)
                    }
                    WizardStep.Review -> Page(R.string.wizard_review_title, R.string.wizard_review_body) {
                        Review(
                            draft = draft,
                            suggestedName = viewModel.suggestedName(fallbackName),
                            appLabels = draft.packages.map(viewModel::appLabel),
                            matches = preview,
                            recentCount = recentItems.size,
                            onChange = viewModel::update,
                        )
                    }
                }
            }
        }
    }
}

/** A question as a big title, one line of help, then the page's controls. */
@Composable
private fun Page(@StringRes titleRes: Int, @StringRes bodyRes: Int, scrollable: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier),
    ) {
        Text(
            stringResource(titleRes),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp),
        )
        Hint(stringResource(bodyRes), Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 20.dp))
        content()
        if (scrollable) Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectedApps(draft: Rule, labelOf: (String) -> String, onRemove: (String) -> Unit) {
    AnimatedVisibility(visible = draft.packages.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            draft.packages.forEach { pkg ->
                RemovablePill(labelOf(pkg), onRemove = { onRemove(pkg) }, leading = { AppIcon(pkg, size = 18.dp) })
            }
        }
    }
}

private enum class AlertChoice { Default, Custom, Silent }

@Composable
private fun AlertChoices(draft: Rule, defaultAlert: AlertProfile, onChange: ((Rule) -> Rule) -> Unit) {
    val choice = when {
        draft.useDefaultAlert -> AlertChoice.Default
        draft.alert.isSilent -> AlertChoice.Silent
        else -> AlertChoice.Custom
    }
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ChoiceCard(
            selected = choice == AlertChoice.Default,
            title = stringResource(R.string.rule_alert_use_default),
            subtitle = profileSummary(defaultAlert),
            onClick = { onChange { it.copy(useDefaultAlert = true) } },
        )
        ChoiceCard(
            selected = choice == AlertChoice.Custom,
            title = stringResource(R.string.wizard_alert_custom),
            subtitle = stringResource(R.string.wizard_alert_custom_desc),
            onClick = {
                onChange { r -> r.copy(useDefaultAlert = false, alert = if (r.alert.isSilent) AlertProfile() else r.alert) }
            },
        )
        AnimatedVisibility(visible = choice == AlertChoice.Custom) {
            SectionCard {
                AlertProfileEditor(profile = draft.alert, onChange = { profile -> onChange { it.copy(alert = profile) } })
            }
        }
        ChoiceCard(
            selected = choice == AlertChoice.Silent,
            title = stringResource(R.string.rule_alert_summary_silent),
            subtitle = stringResource(R.string.alert_silent_desc),
            onClick = { onChange { it.copy(useDefaultAlert = false, alert = AlertPolicy.SILENT) } },
        )
        SectionCard(modifier = Modifier.padding(top = 8.dp)) {
            SwitchRow(
                title = stringResource(R.string.rule_bypass_quiet),
                subtitle = stringResource(R.string.rule_bypass_quiet_desc),
                checked = draft.bypassQuietHours,
                onCheckedChange = { on -> onChange { it.copy(bypassQuietHours = on) } },
            )
            CardDivider()
            SwitchRow(
                title = stringResource(R.string.double_tidy_title),
                subtitle = stringResource(R.string.double_tidy_desc),
                checked = draft.dismissOriginal,
                onCheckedChange = { on -> onChange { it.copy(dismissOriginal = on) } },
            )
        }
    }
}

@Composable
private fun ChoiceCard(selected: Boolean, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.5.dp, Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 4.dp, end = 16.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.muted)
            }
        }
    }
}

@Composable
private fun Review(
    draft: Rule,
    suggestedName: String,
    appLabels: List<String>,
    matches: List<ParsedNotification>,
    recentCount: Int,
    onChange: ((Rule) -> Rule) -> Unit,
) {
    val separator = stringResource(R.string.list_separator)
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = draft.name,
                onValueChange = { name -> onChange { it.copy(name = name) } },
                label = { Text(stringResource(R.string.rule_name_label)) },
                placeholder = { Text(suggestedName) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = draft.tag.orEmpty(),
                onValueChange = { tag -> onChange { it.copy(tag = tag) } },
                label = { Text(stringResource(R.string.rule_tag_label)) },
                placeholder = { Text(stringResource(R.string.rule_tag_placeholder)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionCard(title = stringResource(R.string.wizard_summary)) {
            SummaryLine(stringResource(R.string.rule_apps_label), appLabels.joinToString(separator).ifEmpty { stringResource(R.string.rule_any_app) })
            CardDivider()
            SummaryLine(stringResource(R.string.rule_senders_label), draft.senders.joinToString(separator).ifEmpty { stringResource(R.string.rule_anyone) })
            CardDivider()
            SummaryLine(
                stringResource(R.string.rule_keywords_label),
                draft.keywords.joinToString(separator) { "“$it”" }.ifEmpty { stringResource(R.string.rule_any_content) },
            )
            CardDivider()
            SummaryLine(stringResource(R.string.rule_alert_label), alertSummary(draft))
        }

        SectionCard(title = stringResource(R.string.rule_preview_title)) {
            Hint(
                if (recentCount == 0) {
                    stringResource(R.string.rule_preview_no_recent)
                } else {
                    pluralStringResource(R.plurals.rule_preview_summary, matches.size, matches.size, recentCount)
                },
                Modifier.padding(vertical = 12.dp),
            )
            matches.take(3).forEach { notification ->
                CardDivider()
                NotificationRow(notification)
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(Modifier.padding(vertical = 12.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.muted, modifier = Modifier.width(110.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

/** Back on the left, the main action on the right; rides above the keyboard. */
@Composable
private fun WizardBar(showBack: Boolean, primaryLabel: String, onBack: () -> Unit, onPrimary: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBack) TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
            Spacer(Modifier.weight(1f))
            Button(onClick = onPrimary, shape = MaterialTheme.shapes.large, modifier = Modifier.height(48.dp)) {
                Text(primaryLabel)
            }
        }
    }
}
