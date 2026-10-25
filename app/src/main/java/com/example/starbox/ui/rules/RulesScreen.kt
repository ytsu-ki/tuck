package com.example.starbox.ui.rules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.starbox.LocalAppContainer
import com.example.starbox.R
import com.example.starbox.data.model.Rule
import com.example.starbox.rules.RuleEngine
import com.example.starbox.rules.RuleGroupKey
import com.example.starbox.ui.components.AppIcon
import com.example.starbox.ui.components.CardDivider
import com.example.starbox.ui.components.EmptyState
import com.example.starbox.ui.components.Hint
import com.example.starbox.ui.components.SectionCard
import com.example.starbox.ui.components.TabScreen
import com.example.starbox.ui.components.TagPill
import com.example.starbox.ui.theme.muted

/**
 * Rules grouped by the app they watch. Each row is one compact line of "what" plus one of
 * "how it alerts / how often it fires"; everything else lives on the rule's own page.
 */
@Composable
fun RulesScreen(
    contentPadding: PaddingValues,
    onOpenRule: (Long) -> Unit,
    onNewRule: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: RulesViewModel = viewModel(factory = RulesViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()

    TabScreen(
        title = stringResource(R.string.tab_rules),
        contentPadding = contentPadding,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewRule,
                shape = MaterialTheme.shapes.large,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.rule_new)) },
            )
        },
    ) { inner ->
        val current = state
        when {
            current == null -> Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            current.groups.isEmpty() -> EmptyState(
                icon = Icons.AutoMirrored.Filled.List,
                title = stringResource(R.string.rules_empty_title),
                body = stringResource(R.string.rules_empty_body),
                modifier = Modifier.padding(inner),
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = inner.calculateTopPadding(),
                    // Leave room so the FAB never covers the last switch.
                    bottom = 104.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item(key = "summary") {
                    Hint(
                        listOfNotNull(
                            pluralStringResource(R.plurals.rules_active_count, current.activeCount, current.activeCount),
                            current.pausedCount.takeIf { it > 0 }
                                ?.let { pluralStringResource(R.plurals.rules_paused_count, it, it) },
                        ).joinToString(" · "),
                        Modifier
                            .animateItem()
                            .padding(start = 4.dp),
                    )
                }
                items(current.groups, key = { it.key.toString() }) { group ->
                    RuleGroupCard(
                        group = group,
                        onOpen = onOpenRule,
                        onEnabledChange = viewModel::setEnabled,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleGroupCard(
    group: RuleListGroup,
    onOpen: (Long) -> Unit,
    onEnabledChange: (Rule, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (group.key) {
        is RuleGroupKey.App -> group.appLabel.orEmpty()
        RuleGroupKey.SeveralApps -> stringResource(R.string.rule_group_several_apps)
        RuleGroupKey.AnyApp -> stringResource(R.string.rule_group_any_app)
    }
    SectionCard(
        modifier = modifier,
        title = "$title · ${group.items.size}",
        leading = { GroupIcon(group) },
    ) {
        group.items.forEachIndexed { index, item ->
            if (index > 0) CardDivider()
            RuleRow(
                item = item,
                // The group already names the app; only multi-app rules need to list theirs.
                showApps = group.key == RuleGroupKey.SeveralApps,
                onClick = { onOpen(item.rule.id) },
                onEnabledChange = { onEnabledChange(item.rule, it) },
            )
        }
    }
}

@Composable
private fun GroupIcon(group: RuleListGroup) {
    when (val key = group.key) {
        is RuleGroupKey.App -> AppIcon(key.packageName, size = 20.dp)
        RuleGroupKey.SeveralApps -> {
            // Two overlapping icons hint at "more than one app".
            val packages = group.items.flatMap { it.rule.packages }.distinct().take(2)
            Box(Modifier.width(30.dp)) {
                packages.forEachIndexed { i, pkg -> AppIcon(pkg, size = 18.dp, modifier = Modifier.offset(x = (i * 11).dp)) }
            }
        }
        RuleGroupKey.AnyApp -> Icon(
            Icons.Filled.Notifications,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun RuleRow(item: RuleListItem, showApps: Boolean, onClick: () -> Unit, onEnabledChange: (Boolean) -> Unit) {
    val rule = item.rule
    val broken = !RuleEngine.hasConditions(rule)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Dim a paused rule's text but keep its switch fully visible.
        Column(
            Modifier
                .weight(1f)
                .alpha(if (rule.enabled) 1f else 0.5f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    rule.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                rule.tag?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.width(8.dp))
                    TagPill(it)
                }
            }
            Text(
                conditionSummary(rule, item.appLabels, includeApps = showApps),
                style = MaterialTheme.typography.bodySmall,
                color = if (broken) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                alertSummary(rule) + " · " + statsSummary(item.stats),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = rule.enabled, onCheckedChange = onEnabledChange)
    }
}
