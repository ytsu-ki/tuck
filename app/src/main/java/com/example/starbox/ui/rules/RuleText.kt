package com.example.starbox.ui.rules

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.starbox.R
import com.example.starbox.data.model.AlertProfile
import com.example.starbox.data.model.Rule
import com.example.starbox.rules.RuleStats

/** Short labeled clauses such as "From Alice, Bob · Mentions “urgent”". */
@Composable
fun conditionSummary(rule: Rule, appLabels: List<String>, includeApps: Boolean): String {
    val separator = stringResource(R.string.list_separator)
    val clauses = buildList {
        if (includeApps && appLabels.isNotEmpty()) add(stringResource(R.string.rule_clause_apps, appLabels.joinToString(separator)))
        if (rule.senders.isNotEmpty()) add(stringResource(R.string.rule_clause_from, rule.senders.joinToString(separator)))
        if (rule.keywords.isNotEmpty()) {
            add(stringResource(R.string.rule_clause_mentions, rule.keywords.joinToString(separator) { "“$it”" }))
        }
    }
    return when {
        clauses.isNotEmpty() -> clauses.joinToString(" · ")
        rule.packages.isNotEmpty() -> stringResource(R.string.rule_clause_everything)
        else -> stringResource(R.string.rule_no_conditions)
    }
}

/** "Default alert", "Silent" or "Sound · Banner", plus a note when it alerts in quiet hours. */
@Composable
fun alertSummary(rule: Rule): String {
    val base = if (rule.useDefaultAlert) stringResource(R.string.rule_alert_summary_default) else profileSummary(rule.alert)
    return listOfNotNull(
        base,
        stringResource(R.string.rule_alert_summary_quiet).takeIf { rule.bypassQuietHours },
        stringResource(R.string.rule_alert_summary_tidy).takeIf { rule.dismissOriginal },
    ).joinToString(" · ")
}

/** "Sound · Vibrate · Banner" or "Silent". */
@Composable
fun profileSummary(profile: AlertProfile): String =
    if (profile.isSilent) {
        stringResource(R.string.rule_alert_summary_silent)
    } else {
        listOfNotNull(
            stringResource(R.string.alert_sound).takeIf { profile.sound },
            stringResource(R.string.alert_vibrate).takeIf { profile.vibrate },
            stringResource(R.string.alert_banner).takeIf { profile.headsUp },
        ).joinToString(" · ")
    }

/** "12 starred · 2 hr. ago" or "No matches yet". */
@Composable
fun statsSummary(stats: RuleStats): String {
    val last = stats.lastStarredAt ?: return stringResource(R.string.rule_stats_none)
    return pluralStringResource(R.plurals.rule_stats_starred, stats.starredCount, stats.starredCount) + " · " +
        relativeTime(last)
}

fun relativeTime(time: Long): String = DateUtils.getRelativeTimeSpanString(
    time,
    System.currentTimeMillis(),
    DateUtils.MINUTE_IN_MILLIS,
    DateUtils.FORMAT_ABBREV_RELATIVE,
).toString()
