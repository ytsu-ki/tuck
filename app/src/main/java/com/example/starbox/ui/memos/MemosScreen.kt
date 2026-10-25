package com.example.starbox.ui.memos

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.starbox.LocalAppContainer
import com.example.starbox.R
import com.example.starbox.data.model.Memo
import com.example.starbox.ui.components.AppIcon
import com.example.starbox.ui.components.EmptyState
import com.example.starbox.ui.components.Segment
import com.example.starbox.ui.components.SegmentedTabs
import com.example.starbox.ui.components.SoftFilterChip
import com.example.starbox.ui.components.TabScreen
import com.example.starbox.ui.components.TagPill
import com.example.starbox.ui.theme.StarBoxTheme
import com.example.starbox.ui.theme.muted

/** Ideas are tinted with the tertiary color and a lightbulb; to-dos with the accent and a check. */
@Composable
fun ideaTint(): Color = MaterialTheme.colorScheme.tertiary

@Composable
fun todoTint(): Color = MaterialTheme.colorScheme.primary

@Composable
fun MemosScreen(
    contentPadding: PaddingValues,
    onOpenMemo: (Long) -> Unit,
    onNewMemo: (asTodo: Boolean) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: MemosViewModel = viewModel(factory = MemosViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tab = state?.tab ?: MemoTab.Ideas

    TabScreen(
        title = stringResource(R.string.tab_memos),
        contentPadding = contentPadding,
        floatingActionButton = {
            FloatingActionButton(onClick = { onNewMemo(tab == MemoTab.Todos) }, shape = MaterialTheme.shapes.large) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(if (tab == MemoTab.Todos) R.string.memo_new_todo else R.string.memo_new_idea),
                )
            }
        },
    ) { inner ->
        val current = state
        Column(Modifier.padding(inner)) {
            SegmentedTabs(
                segments = listOf(
                    Segment(
                        stringResource(R.string.memo_tab_ideas),
                        painterResource(R.drawable.ic_lightbulb),
                        ideaTint(),
                        current?.ideaCount,
                    ),
                    Segment(
                        stringResource(R.string.memo_tab_todos),
                        rememberVectorPainter(Icons.Filled.CheckCircle),
                        todoTint(),
                        current?.openTodoCount,
                    ),
                ),
                selectedIndex = tab.ordinal,
                onSelect = { viewModel.setTab(MemoTab.entries[it]) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            QuickCapture(tab = tab, onAdd = viewModel::quickAdd)
            if (current == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }
            if (current.tags.isNotEmpty()) TagFilterBar(current, viewModel)
            AnimatedContent(
                targetState = current.tab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "memoTab",
            ) { shownTab ->
                when (shownTab) {
                    MemoTab.Ideas -> IdeasGrid(current.ideas, onOpenMemo)
                    MemoTab.Todos -> TodoList(
                        open = current.openTodos,
                        done = current.doneTodos,
                        onOpen = onOpenMemo,
                        onDoneChange = viewModel::setDone,
                    )
                }
            }
        }
    }
}

/** A floating, pill-shaped input whose icon and hint follow the open tab. */
@Composable
private fun QuickCapture(tab: MemoTab, onAdd: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    fun submit() {
        if (text.isBlank()) return
        onAdd(text)
        text = ""
    }
    val isTodo = tab == MemoTab.Todos

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier
                .heightIn(min = 56.dp)
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (isTodo) painterResource(R.drawable.ic_circle_outline) else painterResource(R.drawable.ic_lightbulb),
                contentDescription = null,
                tint = if (isTodo) todoTint() else ideaTint(),
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (text.isEmpty()) {
                    Text(
                        stringResource(if (isTodo) R.string.memo_quick_todo_hint else R.string.memo_quick_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.muted,
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AnimatedVisibility(visible = text.isNotBlank(), enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                FilledIconButton(
                    onClick = ::submit,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isTodo) todoTint() else ideaTint(),
                    ),
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(if (isTodo) R.string.memo_new_todo else R.string.memo_new_idea),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TagFilterBar(state: MemosUiState, viewModel: MemosViewModel) {
    LazyRow(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.tags, key = { it }) { tag ->
            val selected = state.tag == tag
            SoftFilterChip(
                selected = selected,
                onClick = { viewModel.setTag(if (selected) null else tag) },
                label = "#$tag",
            )
        }
    }
}

// ---------------------------------------------------------------- Ideas

@Composable
private fun IdeasGrid(ideas: List<MemoRow>, onOpen: (Long) -> Unit) {
    if (ideas.isEmpty()) {
        EmptyState(
            icon = ImageVector.vectorResource(R.drawable.ic_lightbulb),
            title = stringResource(R.string.memo_ideas_empty_title),
            body = stringResource(R.string.memo_ideas_empty_body),
        )
        return
    }
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
        // Leave room so the FAB never covers the last note.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalItemSpacing = 10.dp,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(ideas, key = { it.memo.id }) { row ->
            IdeaCard(row, onClick = { onOpen(row.memo.id) }, modifier = Modifier.animateItem())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdeaCard(row: MemoRow, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val memo = row.memo
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (memo.pinned) StarBoxTheme.star.starContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    if (memo.title.isNotBlank()) {
                        Text(memo.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (memo.body.isNotBlank()) {
                        Text(
                            memo.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = if (memo.title.isNotBlank()) 4.dp else 0.dp),
                        )
                    }
                }
                if (memo.pinned) PinMark(Modifier.padding(start = 6.dp))
            }
            if (memo.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    memo.tags.forEach { TagPill(it) }
                }
            }
            MemoMeta(row, Modifier.padding(top = 12.dp))
        }
    }
}

// ---------------------------------------------------------------- To-dos

@Composable
private fun TodoList(
    open: List<MemoRow>,
    done: List<MemoRow>,
    onOpen: (Long) -> Unit,
    onDoneChange: (Memo, Boolean) -> Unit,
) {
    if (open.isEmpty() && done.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.CheckCircle,
            title = stringResource(R.string.memo_todos_empty_title),
            body = stringResource(R.string.memo_todos_empty_body),
        )
        return
    }
    var showDone by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (open.isEmpty()) {
            item(key = "all-done") {
                Text(
                    stringResource(R.string.memo_todos_all_done),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.muted,
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 4.dp, vertical = 12.dp),
                )
            }
        }
        items(open, key = { it.memo.id }) { row ->
            TodoRow(row, onClick = { onOpen(row.memo.id) }, onDoneChange = { onDoneChange(row.memo, it) }, modifier = Modifier.animateItem())
        }
        if (done.isNotEmpty()) {
            item(key = "done-header") {
                Row(
                    Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .clickable { showDone = !showDone }
                        .padding(start = 4.dp, top = 16.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.memo_todos_done_header, done.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.muted,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        if (showDone) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(if (showDone) R.string.action_collapse else R.string.action_expand),
                        tint = MaterialTheme.colorScheme.muted,
                    )
                }
            }
            if (showDone) {
                items(done, key = { it.memo.id }) { row ->
                    TodoRow(row, onClick = { onOpen(row.memo.id) }, onDoneChange = { onDoneChange(row.memo, it) }, modifier = Modifier.animateItem())
                }
            }
        }
    }
}

/** A checklist row: big round check on the left, the task, and quiet metadata underneath. */
@Composable
private fun TodoRow(row: MemoRow, onClick: () -> Unit, onDoneChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val memo = row.memo
    val done = memo.isDone
    val headline = memo.title.ifBlank { memo.body.lineSequence().firstOrNull().orEmpty() }
    val detail = if (memo.title.isNotBlank()) memo.body.trim() else memo.body.lineSequence().drop(1).joinToString(" ").trim()

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onDoneChange(!done) }) {
                Icon(
                    if (done) rememberVectorPainter(Icons.Filled.CheckCircle) else painterResource(R.drawable.ic_circle_outline),
                    contentDescription = stringResource(if (done) R.string.memo_mark_not_done else R.string.memo_mark_done),
                    tint = if (done) todoTint() else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    headline,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                    color = if (done) MaterialTheme.colorScheme.muted else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (detail.isNotEmpty() && !done) {
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!done && (row.source != null || memo.tags.isNotEmpty())) {
                    Row(
                        Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        memo.tags.take(2).forEach { TagPill(it) }
                        row.source?.let { source ->
                            AppIcon(source.packageName, size = 14.dp)
                            Text(
                                source.sender ?: source.appLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            if (memo.pinned && !done) PinMark(Modifier.padding(start = 8.dp))
        }
    }
}

// ---------------------------------------------------------------- Shared bits

@Composable
private fun PinMark(modifier: Modifier = Modifier) {
    Icon(
        painterResource(R.drawable.ic_pin),
        contentDescription = stringResource(R.string.memo_pinned),
        tint = StarBoxTheme.star.star,
        modifier = modifier.size(16.dp),
    )
}

@Composable
private fun MemoMeta(row: MemoRow, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        row.source?.let { source ->
            AppIcon(source.packageName, size = 14.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                source.sender ?: source.appLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(" · ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.muted)
        }
        Text(
            DateUtils.getRelativeTimeSpanString(
                row.memo.updatedAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE,
            ).toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.muted,
            maxLines = 1,
        )
    }
}
