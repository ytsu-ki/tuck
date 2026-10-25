package com.example.starbox.ui.settings

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.starbox.R
import com.example.starbox.ui.components.Hint
import com.example.starbox.ui.theme.AccentColor
import com.example.starbox.ui.theme.StarBoxTheme
import com.example.starbox.ui.theme.accentRoles
import com.example.starbox.ui.theme.wallpaperColorsAvailable

/** Round color swatches; the whole app recolors as soon as one is tapped. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccentPicker(selected: AccentColor, onSelect: (AccentColor) -> Unit) {
    val dark = StarBoxTheme.isDark
    val choices = remember { AccentColor.entries.filter { !it.isDynamic || wallpaperColorsAvailable } }

    Column(Modifier.padding(vertical = 12.dp)) {
        Text(stringResource(R.string.appearance_accent), style = MaterialTheme.typography.bodyLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            choices.forEach { accent ->
                Swatch(
                    fill = if (accent.isDynamic) wallpaperBrush() else Brush.linearGradient(swatchColors(accent, dark)),
                    selected = accent == selected,
                    label = stringResource(accent.label),
                    onClick = { onSelect(accent) },
                )
            }
        }
        Hint(
            stringResource(
                if (selected.isDynamic) R.string.appearance_accent_wallpaper_hint else R.string.appearance_accent_selected,
                stringResource(selected.label),
            ),
            Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun Swatch(fill: Brush, selected: Boolean, label: String, onClick: () -> Unit) {
    val ring by animateDpAsState(if (selected) 3.dp else 0.dp, label = "swatchRing")
    Box(
        Modifier
            .size(44.dp)
            .border(ring, MaterialTheme.colorScheme.onSurface, CircleShape)
            .padding(if (selected) 5.dp else 0.dp)
            .clip(CircleShape)
            .background(fill)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

/** Two tones of the accent so each swatch previews the accent and its container shade. */
private fun swatchColors(accent: AccentColor, dark: Boolean): List<Color> {
    val roles = accentRoles(accent, dark)
    return listOf(Color(roles.star), Color(accentRoles(accent, dark = false).primary))
}

@Composable
private fun wallpaperBrush(): Brush {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return Brush.linearGradient(listOf(Color.Gray, Color.DarkGray))
    val scheme = remember(context) { dynamicLightColorScheme(context) }
    return Brush.sweepGradient(listOf(scheme.primary, scheme.tertiary, scheme.secondary, scheme.primary))
}
