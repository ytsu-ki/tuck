package com.example.starbox.ui.rules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.starbox.R
import com.example.starbox.capture.SourceLoudnessTracker
import com.example.starbox.data.model.Rule
import com.example.starbox.rules.DuplicatePolicy
import com.example.starbox.rules.SourceLoudness
import com.example.starbox.system.Permissions
import com.example.starbox.ui.components.AppIcon
import com.example.starbox.ui.components.CardDivider
import com.example.starbox.ui.components.Hint
import com.example.starbox.ui.components.SectionCard
import com.example.starbox.ui.components.SwitchRow
import com.example.starbox.ui.theme.muted

/**
 * "Avoid double alerts": for each app the rule watches, whether that app still rings by itself
 * and a shortcut to make it Silent, plus the opt-in switch that tidies the original away.
 */
@Composable
fun DoubleAlertsSection(
    rule: Rule,
    appLabels: List<String>,
    samples: Map<String, SourceLoudnessTracker.Sample>,
    onTidyChange: (Boolean) -> Unit,
) {
    SectionCard(title = stringResource(R.string.double_title)) {
        Hint(stringResource(R.string.double_intro), Modifier.padding(top = 12.dp, bottom = 4.dp))
        if (rule.packages.isEmpty()) {
            Hint(stringResource(R.string.double_any_app), Modifier.padding(vertical = 8.dp))
        } else {
            rule.packages.zip(appLabels).forEach { (pkg, label) ->
                CardDivider()
                val sample = samples[pkg]
                val loudness = DuplicatePolicy.loudness(sample?.importance, sample?.sound != false, sample?.vibrates != false)
                SourceAppRow(pkg, label, loudness, sample)
            }
        }
        Hint(stringResource(R.string.double_not_off), Modifier.padding(top = 4.dp, bottom = 10.dp))
        CardDivider()
        SwitchRow(
            title = stringResource(R.string.double_tidy_title),
            subtitle = stringResource(R.string.double_tidy_desc),
            checked = rule.dismissOriginal,
            onCheckedChange = onTidyChange,
        )
    }
}

@Composable
private fun SourceAppRow(
    packageName: String,
    label: String,
    loudness: SourceLoudness,
    sample: SourceLoudnessTracker.Sample?,
) {
    val context = LocalContext.current
    val open = { Permissions.openNotificationSettingsOf(context, packageName, sample?.channelId, sample?.uid) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(packageName, size = 26.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                when (loudness) {
                    SourceLoudness.Loud -> stringResource(R.string.double_status_loud)
                    SourceLoudness.Quiet -> stringResource(R.string.double_status_quiet)
                    SourceLoudness.Unknown -> stringResource(R.string.double_status_unknown, label)
                },
                style = MaterialTheme.typography.bodySmall,
                color = when (loudness) {
                    SourceLoudness.Loud -> MaterialTheme.colorScheme.error
                    SourceLoudness.Quiet -> MaterialTheme.colorScheme.primary
                    SourceLoudness.Unknown -> MaterialTheme.colorScheme.muted
                },
            )
        }
        Spacer(Modifier.width(8.dp))
        when (loudness) {
            SourceLoudness.Loud -> Button(onClick = open) { Text(stringResource(R.string.double_make_quiet)) }
            SourceLoudness.Unknown -> FilledTonalButton(onClick = open) { Text(stringResource(R.string.double_make_quiet)) }
            SourceLoudness.Quiet -> TextButton(onClick = open) { Text(stringResource(R.string.double_settings)) }
        }
    }
}
