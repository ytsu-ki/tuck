package com.example.starbox.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import com.example.starbox.R
import com.example.starbox.data.model.AlertProfile
import com.example.starbox.ui.theme.muted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sound / vibration / banner switches with a ringtone picker, laid out as divided rows for use
 * inside a SectionCard. All off means silent collection.
 */
@Composable
fun AlertProfileEditor(profile: AlertProfile, onChange: (AlertProfile) -> Unit, modifier: Modifier = Modifier) {
    val pickSound = rememberLauncherForActivityResult(PickNotificationSound()) { picked ->
        if (picked != null) onChange(profile.copy(soundUri = picked.uri))
    }
    val soundName = rememberSoundName(profile.soundUri)
    val context = LocalContext.current
    val noPickerText = stringResource(R.string.alert_no_sound_picker)

    Column(modifier) {
        SwitchRow(
            title = stringResource(R.string.alert_sound),
            checked = profile.sound,
            onCheckedChange = { onChange(profile.copy(sound = it)) },
        )
        AnimatedVisibility(visible = profile.sound) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable {
                        try {
                            pickSound.launch(profile.soundUri)
                        } catch (_: ActivityNotFoundException) {
                            // Some trimmed-down phones ship without the system sound picker.
                            Toast.makeText(context, noPickerText, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.alert_choose_sound),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.muted,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    soundName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.muted,
                )
            }
        }
        CardDivider()
        SwitchRow(
            title = stringResource(R.string.alert_vibrate),
            checked = profile.vibrate,
            onCheckedChange = { onChange(profile.copy(vibrate = it)) },
        )
        CardDivider()
        SwitchRow(
            title = stringResource(R.string.alert_banner),
            subtitle = stringResource(R.string.alert_banner_desc),
            checked = profile.headsUp,
            onCheckedChange = { onChange(profile.copy(headsUp = it)) },
        )
        AnimatedVisibility(visible = profile.isSilent) {
            Hint(stringResource(R.string.alert_silent_desc), Modifier.padding(bottom = 12.dp))
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.muted)
            }
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun rememberSoundName(soundUri: String?): String {
    val context = LocalContext.current
    val defaultName = stringResource(R.string.alert_default_sound)
    val name by produceState(defaultName, soundUri) {
        value = if (soundUri == null) {
            defaultName
        } else {
            withContext(Dispatchers.IO) { ringtoneTitle(context, soundUri.toUri()) } ?: defaultName
        }
    }
    return name
}

private fun ringtoneTitle(context: Context, uri: Uri): String? = try {
    RingtoneManager.getRingtone(context, uri)?.getTitle(context)
} catch (_: Exception) {
    // The sound file may have been deleted or be unreadable.
    null
}

/** Result of the system sound picker; a null [uri] means "system default". */
data class PickedSound(val uri: String?)

/** Opens the system notification-sound picker. Returns null if the user cancels. */
class PickNotificationSound : ActivityResultContract<String?, PickedSound?>() {
    override fun createIntent(context: Context, input: String?): Intent =
        Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI)
            .putExtra(
                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                input?.toUri() ?: Settings.System.DEFAULT_NOTIFICATION_URI,
            )

    override fun parseResult(resultCode: Int, intent: Intent?): PickedSound? {
        if (resultCode != Activity.RESULT_OK || intent == null) return null
        val uri = IntentCompat.getParcelableExtra(intent, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        return PickedSound(uri?.takeIf { it != Settings.System.DEFAULT_NOTIFICATION_URI }?.toString())
    }
}
