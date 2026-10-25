package com.example.starbox.ui.memos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.starbox.AppContainer
import com.example.starbox.data.db.MemoDao
import com.example.starbox.data.db.MessageDao
import com.example.starbox.data.model.CapturedMessage
import com.example.starbox.data.model.Memo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ideas (free-form notes) and To-dos (a checklist) are separate views, not one mixed list. */
enum class MemoTab { Ideas, Todos }

data class MemoRow(val memo: Memo, val source: CapturedMessage?)

data class MemosUiState(
    val tab: MemoTab,
    val ideas: List<MemoRow>,
    val openTodos: List<MemoRow>,
    val doneTodos: List<MemoRow>,
    val ideaCount: Int,
    val openTodoCount: Int,
    /** Tags used in the current tab, for its filter chips. */
    val tags: List<String>,
    val tag: String?,
)

class MemosViewModel(
    private val memoDao: MemoDao,
    messageDao: MessageDao,
) : ViewModel() {

    private val tab = MutableStateFlow(MemoTab.Ideas)
    private val tag = MutableStateFlow<String?>(null)

    /** Null until the first load, so the empty state doesn't flash. */
    val state: StateFlow<MemosUiState?> =
        combine(memoDao.observeAll(), messageDao.observeAll(), tab, tag) { memos, messages, currentTab, t ->
            val byId = messages.associateBy { it.id }
            fun row(memo: Memo) = MemoRow(memo, memo.sourceMessageId?.let(byId::get))

            val (todos, ideas) = memos.partition { it.isTodo }
            val inTab = if (currentTab == MemoTab.Todos) todos else ideas
            val tags = inTab.flatMap { it.tags }.distinct().sorted()
            val activeTag = t?.takeIf { it in tags }
            fun matchesTag(memo: Memo) = activeTag == null || activeTag in memo.tags

            MemosUiState(
                tab = currentTab,
                ideas = ideas.filter(::matchesTag)
                    .sortedWith(compareByDescending<Memo> { it.pinned }.thenByDescending { it.updatedAt })
                    .map(::row),
                // Newest to-dos on top so a quick-added one is visible straight away.
                openTodos = todos.filter { !it.isDone && matchesTag(it) }
                    .sortedWith(compareByDescending<Memo> { it.pinned }.thenByDescending { it.createdAt })
                    .map(::row),
                doneTodos = todos.filter { it.isDone && matchesTag(it) }
                    .sortedByDescending { it.updatedAt }
                    .map(::row),
                ideaCount = ideas.size,
                openTodoCount = todos.count { !it.isDone },
                tags = tags,
                tag = activeTag,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setTab(value: MemoTab) {
        if (tab.value == value) return
        tab.value = value
        tag.value = null
    }

    fun setTag(value: String?) {
        tag.value = value
    }

    /** Quick capture: one line becomes an idea or a to-do, depending on the open tab. */
    fun quickAdd(text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        val asTodo = tab.value == MemoTab.Todos
        // A to-do's one line reads best as its title; an idea's as its body.
        val memo = if (asTodo) Memo(title = body, isTodo = true) else Memo(body = body)
        viewModelScope.launch { memoDao.upsert(memo) }
    }

    fun setDone(memo: Memo, done: Boolean) {
        viewModelScope.launch { memoDao.setDone(memo.id, done) }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { MemosViewModel(container.database.memoDao(), container.database.messageDao()) }
        }
    }
}
