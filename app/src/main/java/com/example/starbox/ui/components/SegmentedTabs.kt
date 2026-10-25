package com.example.starbox.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.starbox.ui.theme.muted

/** One option of [SegmentedTabs]; [tint] colors its icon when selected. */
data class Segment(val label: String, val icon: Painter, val tint: Color, val count: Int? = null)

/** A pill-shaped two-or-more-way switch; the selected segment lifts onto a lighter surface. */
@Composable
fun SegmentedTabs(
    segments: List<Segment>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier
                .padding(4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            segments.forEachIndexed { index, segment ->
                val selected = index == selectedIndex
                val background by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.surfaceBright else Color.Transparent,
                    label = "segmentBackground",
                )
                val iconTint by animateColorAsState(
                    if (selected) segment.tint else MaterialTheme.colorScheme.muted,
                    label = "segmentIcon",
                )
                Surface(
                    onClick = { onSelect(index) },
                    shape = CircleShape,
                    color = background,
                    shadowElevation = if (selected) 1.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .semantics {
                            this.selected = selected
                            role = Role.Tab
                        },
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(segment.icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            segment.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.muted,
                        )
                        segment.count?.takeIf { it > 0 }?.let { count ->
                            Spacer(Modifier.width(6.dp))
                            Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.muted)
                        }
                    }
                }
            }
        }
    }
}
