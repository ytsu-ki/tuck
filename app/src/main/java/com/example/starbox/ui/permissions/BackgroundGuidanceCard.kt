package com.example.starbox.ui.permissions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.starbox.R
import com.example.starbox.system.OemBattery
import com.example.starbox.system.Permissions
import com.example.starbox.ui.components.Hint
import com.example.starbox.ui.theme.muted

/**
 * Brand-specific steps to stop the phone from killing StarBox in the background, as an
 * expandable block for use inside a SectionCard.
 */
@Composable
fun BackgroundGuidance(initiallyExpanded: Boolean = false) {
    val context = LocalContext.current
    val oem = remember { OemBattery.current() }
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }

    Column(Modifier.padding(vertical = 6.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.oem_title, stringResource(oem.brandName)),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(if (expanded) R.string.action_collapse else R.string.action_expand),
                tint = MaterialTheme.colorScheme.muted,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(bottom = 8.dp)) {
                Text(stringResource(oem.steps), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Hint(stringResource(R.string.oem_lock_recents))
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (OemBattery.hasSettingsScreen(oem)) {
                        FilledTonalButton(onClick = {
                            if (!OemBattery.openSettings(context, oem)) Permissions.openAppDetails(context)
                        }) { Text(stringResource(R.string.oem_open_settings)) }
                    }
                    TextButton(onClick = { Permissions.openAppDetails(context) }) {
                        Text(stringResource(R.string.perm_open_app_info))
                    }
                }
            }
        }
    }
}
