package com.example.starbox.ui.rules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.starbox.LocalAppContainer
import com.example.starbox.R
import com.example.starbox.data.model.CapturedMessage
import com.example.starbox.rules.RuleEngine
import com.example.starbox.ui.components.AppIcon
import com.example.starbox.ui.components.CardDivider
import com.example.starbox.ui.components.Hint
import com.example.starbox.ui.components.SectionCard
import com.example.starbox.ui.theme.StarBoxTheme
import com.example.starbox.ui.theme.muted

private enum class RuleSheet { Apps, Senders, Keywords, Alert, Tag, Rename }

/**
 * One rule at a glance: status and activity on top, then each condition as a single row.
 * Tapping a row opens a small editor for just that part, so no screen is ever crowded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleDetailScreen(ruleId: Long, onClose: () -> Unit, onOpenRule: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: RuleDetailViewModel = viewModel(factory = RuleDetailViewModel.factory(ruleId, container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val loudness by container.sourceLoudness.samples.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf<RuleSheet?>(null) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val copySuffix = stringResource(R.string.rule_copy_suffix)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Deleted (here or elsewhere): leave the page.
    LaunchedEffect(loaded, state) { if (loaded && state == null) onClose() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            containerColor = StarBoxTheme.floating,
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.rule_rename)) },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    sheet = RuleSheet.Rename
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.rule_duplicate)) },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_copy), contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    viewModel.duplicate(copySuffix, onCreated = onOpenRule)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuOpen = false
                                    confirmDelete = true
                                },
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        val current = state
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val rule = current.rule
        val separator = stringResource(R.string.list_separator)

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Header(
                name = rule.name,
                packages = rule.packages,
                enabled = rule.enabled,
                onRename = { sheet = RuleSheet.Rename },
                onEnabledChange = { on -> viewModel.update { it.copy(enabled = on) } },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    value = current.stats.starredCount.toString(),
                    label = stringResource(R.string.rule_stat_starred),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = current.stats.lastStarredAt?.let(::relativeTime) ?: "—",
                    label = stringResource(R.string.rule_stat_last),
                    modifier = Modifier.weight(1f),
                )
            }

            if (!RuleEngine.hasConditions(rule)) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.rule_no_conditions_warning), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            SectionCard(title = stringResource(R.string.rule_section_match)) {
                DetailRow(
                    icon = {
                        if (rule.packages.isNotEmpty()) {
                            AppIcon(rule.packages.first(), size = 22.dp)
                        } else {
                            RowIcon(Icons.Filled.Notifications)
                        }
                    },
                    label = stringResource(R.string.rule_apps_label),
                    value = current.appLabels.joinToString(separator).ifEmpty { stringResource(R.string.rule_any_app) },
                    onClick = { sheet = RuleSheet.Apps },
                )
                CardDivider()
                DetailRow(
                    icon = { RowIcon(Icons.Filled.Person) },
                    label = stringResource(R.string.rule_senders_label),
                    value = rule.senders.joinToString(separator).ifEmpty { stringResource(R.string.rule_anyone) },
                    onClick = { sheet = RuleSheet.Senders },
                )
                CardDivider()
                DetailRow(
                    icon = { RowIcon(Icons.Filled.Search) },
                    label = stringResource(R.string.rule_keywords_label),
                    value = rule.keywords.joinToString(separator) { "“$it”" }.ifEmpty { stringResource(R.string.rule_any_content) },
                    onClick = { sheet = RuleSheet.Keywords },
                )
                Hint(
                    buildString {
                        append(stringResource(R.string.rule_and_or_hint))
                        if (current.recentCount > 0 && RuleEngine.hasConditions(rule)) {
                            append(' ')
                            append(
                                pluralStringResource(
                                    R.plurals.rule_preview_summary,
                                    current.previewMatches,
                                    current.previewMatches,
                                    current.recentCount,
                                ),
                            )
                        }
                    },
                    Modifier.padding(bottom = 14.dp, top = 2.dp),
                )
            }

            SectionCard(title = stringResource(R.string.rule_alert_label)) {
                DetailRow(
                    icon = { RowIcon(Icons.Filled.Notifications) },
                    label = stringResource(R.string.rule_alert_label),
                    value = alertSummary(rule),
                    onClick = { sheet = RuleSheet.Alert },
                )
                CardDivider()
                DetailRow(
                    icon = { RowIcon(text = "#") },
                    label = stringResource(R.string.rule_tag_label),
                    value = rule.tag?.let { "#$it" } ?: stringResource(R.string.rule_no_tag),
                    onClick = { sheet = RuleSheet.Tag },
                )
            }

            DoubleAlertsSection(
                rule = rule,
                appLabels = current.appLabels,
                samples = loudness,
                onTidyChange = { on -> viewModel.update { it.copy(dismissOriginal = on) } },
            )

            SectionCard(title = stringResource(R.string.rule_recent_matches)) {
                if (current.recentMatches.isEmpty()) {
                    Hint(stringResource(R.string.rule_recent_matches_empty), Modifier.padding(vertical = 14.dp))
                } else {
                    current.recentMatches.forEachIndexed { index, message ->
                        if (index > 0) CardDivider()
                        MatchRow(message)
                    }
                }
            }
        }

        when (sheet) {
            RuleSheet.Apps -> AppPickerSheet(
                apps = installedApps,
                selected = rule.packages,
                onToggle = { pkg ->
                    viewModel.update { r -> r.copy(packages = if (pkg in r.packages) r.packages - pkg else r.packages + pkg) }
                },
                onDismiss = { sheet = null },
            )
            RuleSheet.Senders -> ValuesSheet(
                title = stringResource(R.string.rule_senders_label),
                description = stringResource(R.string.rule_senders_hint),
                label = stringResource(R.string.rule_senders_label),
                placeholder = stringResource(R.string.rule_senders_placeholder),
                initial = rule.senders,
                suggestions = viewModel::senderSuggestions,
                onDone = { values ->
                    viewModel.update { it.copy(senders = values) }
                    sheet = null
                },
            )
            RuleSheet.Keywords -> ValuesSheet(
                title = stringResource(R.string.rule_keywords_label),
                description = stringResource(R.string.rule_keywords_hint),
                label = stringResource(R.string.rule_keywords_label),
                placeholder = stringResource(R.string.rule_keywords_placeholder),
                initial = rule.keywords,
                suggestions = { emptyList() },
                onDone = { values ->
                    viewModel.update { it.copy(keywords = values) }
                    sheet = null
                },
            )
            RuleSheet.Alert -> AlertSheet(rule = rule, onChange = viewModel::update, onDismiss = { sheet = null })
            RuleSheet.Tag -> TagDialog(
                current = rule.tag,
                existing = current.allTags,
                onDismiss = { sheet = null },
                onSave = { tag ->
                    viewModel.update { it.copy(tag = tag) }
                    sheet = null
                },
            )
            RuleSheet.Rename -> RenameDialog(
                current = rule.name,
                onDismiss = { sheet = null },
                onRename = { name ->
                    viewModel.update { it.copy(name = name) }
                    sheet = null
                },
            )
            null -> Unit
        }
    }

    if (confirmDelete) {
        AlertDialog(
            containerColor = StarBoxTheme.floating,
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.rule_delete_title)) },
            text = { Text(stringResource(R.string.rule_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onDone = onClose)
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun Header(
    name: String,
    packages: List<String>,
    enabled: Boolean,
    onRename: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Surface(shape = CircleShape, color = StarBoxTheme.star.starContainer, modifier = Modifier.size(52.dp)) {
            Box(contentAlignment = Alignment.Center) {
                if (packages.size == 1) {
                    AppIcon(packages.single(), size = 28.dp)
                } else {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = StarBoxTheme.star.star, modifier = Modifier.size(26.dp))
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(
            Modifier
                .weight(1f)
                .clickable(onClick = onRename),
        ) {
            Text(name, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(if (enabled) R.string.rule_status_active else R.string.rule_status_paused),
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.muted,
            )
        }
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = modifier) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.muted)
        }
    }
}

/** One tappable condition: icon, label, current value, chevron. */
@Composable
private fun DetailRow(icon: @Composable () -> Unit, label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.muted)
    }
}

@Composable
private fun RowIcon(vector: ImageVector? = null, text: String? = null) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(36.dp)) {
        Box(contentAlignment = Alignment.Center) {
            if (vector != null) {
                Icon(vector, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
            } else if (text != null) {
                Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

@Composable
private fun MatchRow(message: CapturedMessage) {
    Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIcon(message.packageName, size = 20.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                listOfNotNull(message.sender ?: message.title, message.conversation).joinToString(" · ").ifEmpty { message.appLabel },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                message.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(relativeTime(message.postedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.muted)
    }
}
