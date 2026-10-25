package com.example.starbox.ui.memos

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.starbox.LocalAppContainer
import com.example.starbox.R
import com.example.starbox.data.model.CapturedMessage
import com.example.starbox.ui.components.AppIcon
import com.example.starbox.ui.components.ChipInputField
import com.example.starbox.ui.components.Segment
import com.example.starbox.ui.components.SegmentedTabs
import com.example.starbox.ui.theme.StarBoxTheme
import com.example.starbox.ui.theme.muted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoEditorScreen(memoId: Long, fromMessageId: Long, initialTodo: Boolean, onClose: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val quoteTemplate = stringResource(R.string.memo_quote_format)
    val viewModel: MemoEditorViewModel = viewModel(
        factory = MemoEditorViewModel.factory(memoId, fromMessageId, initialTodo, container) { message ->
            val who = listOfNotNull(message.sender ?: message.title, message.conversation)
                .joinToString(" · ")
                .ifEmpty { message.appLabel }
            val where = message.appLabel + ", " + DateUtils.formatDateTime(
                context,
                message.postedAt,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
            )
            quoteTemplate.format(message.text, who, where)
        },
    )
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val source by viewModel.source.collectAsStateWithLifecycle()
    var tagInput by rememberSaveable { mutableStateOf("") }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val close = { viewModel.saveAndClose(tagInput, onClose) }
    BackHandler(onBack = close)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val todo = draft?.isTodo ?: initialTodo
                    Text(
                        stringResource(
                            when {
                                viewModel.isNew && todo -> R.string.memo_new_todo
                                viewModel.isNew -> R.string.memo_new_idea
                                todo -> R.string.memo_tab_todo_single
                                else -> R.string.memo_tab_idea_single
                            },
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = close) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    draft?.let { memo ->
                        IconButton(onClick = { viewModel.update { it.copy(pinned = !it.pinned) } }) {
                            Icon(
                                painterResource(R.drawable.ic_pin),
                                contentDescription = stringResource(if (memo.pinned) R.string.memo_unpin else R.string.memo_pin),
                                tint = if (memo.pinned) StarBoxTheme.star.star else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (!viewModel.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        val memo = draft
        if (memo == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        // Kept locally so the cursor starts after a quoted message instead of before it.
        var body by rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue(memo.body, TextRange(memo.body.length)))
        }
        val bodyFocus = remember { FocusRequester() }
        val titleFocus = remember { FocusRequester() }
        // A new to-do starts at its title ("what needs doing?"); a new idea at its body.
        LaunchedEffect(Unit) {
            if (viewModel.isNew) (if (memo.isTodo) titleFocus else bodyFocus).requestFocus()
        }

        val plain = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
        ) {
            // The type is the first choice on the page, with its own icon and color.
            SegmentedTabs(
                segments = listOf(
                    Segment(stringResource(R.string.memo_tab_idea_single), painterResource(R.drawable.ic_lightbulb), ideaTint()),
                    Segment(stringResource(R.string.memo_tab_todo_single), rememberVectorPainter(Icons.Filled.CheckCircle), todoTint()),
                ),
                selectedIndex = if (memo.isTodo) 1 else 0,
                onSelect = { index -> viewModel.update { it.copy(isTodo = index == 1) } },
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(visible = memo.isTodo, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
                    IconButton(onClick = { viewModel.update { it.copy(isDone = !it.isDone) } }) {
                        Icon(
                            if (memo.isDone) rememberVectorPainter(Icons.Filled.CheckCircle) else painterResource(R.drawable.ic_circle_outline),
                            contentDescription = stringResource(if (memo.isDone) R.string.memo_mark_not_done else R.string.memo_mark_done),
                            tint = if (memo.isDone) todoTint() else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                TextField(
                    value = memo.title,
                    onValueChange = { title -> viewModel.update { it.copy(title = title) } },
                    placeholder = {
                        Text(
                            stringResource(if (memo.isTodo) R.string.memo_todo_title_hint else R.string.memo_title_hint),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        textDecoration = if (memo.isTodo && memo.isDone) TextDecoration.LineThrough else null,
                    ),
                    colors = plain,
                    singleLine = !memo.isTodo,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(titleFocus),
                )
            }
            TextField(
                value = body,
                onValueChange = { value ->
                    body = value
                    viewModel.update { it.copy(body = value.text) }
                },
                placeholder = {
                    Text(stringResource(if (memo.isTodo) R.string.memo_todo_body_hint else R.string.memo_body_hint))
                },
                colors = plain,
                minLines = if (memo.isTodo) 3 else 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(bodyFocus),
            )
            ChipInputField(
                label = stringResource(R.string.memo_tags_label),
                placeholder = stringResource(R.string.rule_tag_placeholder),
                values = memo.tags,
                input = tagInput,
                onInputChange = { tagInput = it },
                onValuesChange = { tags -> viewModel.update { it.copy(tags = tags) } },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            source?.let { SourceCard(it) }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            containerColor = StarBoxTheme.floating,
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.memo_delete_title)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onClose)
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

/** Shows which starred message a memo was made from. */
@Composable
private fun SourceCard(message: CapturedMessage) {
    val context = LocalContext.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(message.packageName, size = 20.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.memo_from_message, message.sender ?: message.title ?: message.appLabel),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    message.appLabel + " · " + DateUtils.formatDateTime(
                        context,
                        message.postedAt,
                        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.muted,
                )
            }
        }
    }
}
