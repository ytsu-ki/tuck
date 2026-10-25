package com.example.starbox.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.starbox.capture.ParsedNotification
import com.example.starbox.ui.theme.StarBoxTheme
import com.example.starbox.ui.theme.muted

/** Compact view of a notification StarBox has read, with the rules that star it. */
@Composable
fun NotificationRow(
    notification: ParsedNotification,
    modifier: Modifier = Modifier,
    matchedRuleNames: List<String> = emptyList(),
) {
    Column(modifier.padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(notification.packageName, size = 16.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                notification.appLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.muted,
                modifier = Modifier.weight(1f),
            )
            Text(
                DateUtils.getRelativeTimeSpanString(
                    notification.postedAt,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE,
                ).toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.muted,
            )
        }
        val heading = listOfNotNull(notification.sender, notification.conversation).joinToString(" · ")
        if (heading.isNotEmpty()) {
            Text(
                heading,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (notification.text.isNotEmpty()) {
            Text(
                notification.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (matchedRuleNames.isNotEmpty()) {
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                matchedRuleNames.take(3).forEach { RulePill(it) }
            }
        }
    }
}

/** Amber "★ rule name" pill marking what starred a message. */
@Composable
fun RulePill(name: String, modifier: Modifier = Modifier) {
    Pill(
        text = "★ $name",
        container = StarBoxTheme.star.starContainer,
        content = StarBoxTheme.star.onStarContainer,
        modifier = modifier,
    )
}

/** Neutral "#tag" pill. */
@Composable
fun TagPill(tag: String, modifier: Modifier = Modifier) {
    Pill(
        text = "#$tag",
        container = MaterialTheme.colorScheme.secondaryContainer,
        content = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier,
    )
}
