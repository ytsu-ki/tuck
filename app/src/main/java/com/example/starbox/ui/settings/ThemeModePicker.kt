package com.example.starbox.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.starbox.R
import com.example.starbox.ui.components.Segment
import com.example.starbox.ui.components.SegmentedTabs
import com.example.starbox.ui.theme.ThemeMode

/** System / Light / Dark as one three-way switch. */
@Composable
fun ThemeModePicker(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Column(Modifier.padding(vertical = 12.dp)) {
        Text(stringResource(R.string.appearance_theme), style = MaterialTheme.typography.bodyLarge)
        SegmentedTabs(
            segments = ThemeMode.entries.map { mode ->
                Segment(stringResource(mode.label), painterResource(mode.icon), MaterialTheme.colorScheme.primary)
            },
            selectedIndex = selected.ordinal,
            onSelect = { onSelect(ThemeMode.entries[it]) },
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
