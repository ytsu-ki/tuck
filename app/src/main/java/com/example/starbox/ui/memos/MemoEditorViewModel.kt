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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Edits one memo. A new memo can start from an inbox message, in which case its body opens
 * with a quote of that message and the memo keeps a link to it.
 */
class MemoEditorViewModel(
    private val memoId: Long,
    private val fromMessageId: Long,
    private val initialTodo: Boolean,
    private val memoDao: MemoDao,
    private val messageDao: MessageDao,
    private val quote: (CapturedMessage) -> String,
    /** Outlives this screen, for the save in [onCleared]. */
    private val appScope: CoroutineScope,
) : ViewModel() {

    val isNew = memoId == NEW_MEMO_ID

    /** Null while loading. */
    private val _draft = MutableStateFlow<Memo?>(null)
    val draft: StateFlow<Memo?> = _draft.asStateFlow()

    private val _source = MutableStateFlow<CapturedMessage?>(null)
    val source: StateFlow<CapturedMessage?> = _source.asStateFlow()

    private var closing = false

    init {
        viewModelScope.launch {
            val memo = when {
                !isNew -> memoDao.getById(memoId) ?: Memo()
                fromMessageId > 0 -> messageDao.getById(fromMessageId)
                    ?.let { Memo(body = quote(it), sourceMessageId = it.id) }
                    ?: Memo()
                else -> Memo(isTodo = initialTodo)
            }
            _source.value = memo.sourceMessageId?.let { messageDao.getById(it) }
            _draft.value = memo
        }
    }

    fun update(transform: (Memo) -> Memo) {
        _draft.update { it?.let(transform) }
    }

    /**
     * Saves and then calls [onDone]. An empty memo is not kept: a new one is dropped and an
     * existing one is deleted, so backing out of a blank editor leaves nothing behind.
     */
    fun saveAndClose(pendingTag: String, onDone: () -> Unit) {
        // A double tap on Back must not insert a new memo twice.
        if (closing) return
        closing = true
        val current = _draft.value ?: return onDone()
        viewModelScope.launch {
            withContext(NonCancellable) { persist(current, pendingTag) }
            onDone()
        }
    }

    /**
     * Leaving without Back or the arrow (tapping a Tuck alert jumps to the inbox and drops this
     * screen, or the app is closed from Recents) must not lose what was typed.
     */
    override fun onCleared() {
        if (closing) return
        val current = _draft.value ?: return
        appScope.launch { persist(current, pendingTag = "") }
    }

    private suspend fun persist(current: Memo, pendingTag: String) {
        val memo = current.copy(
            title = current.title.trim(),
            body = current.body.trimEnd(),
            tags = (current.tags + pendingTag.split(',', '，')).map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
            isDone = current.isTodo && current.isDone,
        )
        when {
            memo.title.isEmpty() && memo.body.isEmpty() -> if (!isNew) memoDao.delete(memo)
            memo != loadedOriginal() -> memoDao.upsert(memo.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun delete(onDone: () -> Unit) {
        if (closing) return
        closing = true
        val current = _draft.value ?: return onDone()
        viewModelScope.launch {
            withContext(NonCancellable) { if (!isNew) memoDao.delete(current) }
            onDone()
        }
    }

    /** Unchanged memos keep their position in the list instead of jumping to the top. */
    private suspend fun loadedOriginal(): Memo? = if (isNew) null else memoDao.getById(memoId)

    companion object {
        const val NEW_MEMO_ID = 0L

        fun factory(
            memoId: Long,
            fromMessageId: Long,
            initialTodo: Boolean,
            container: AppContainer,
            quote: (CapturedMessage) -> String,
        ) =
            viewModelFactory {
                initializer {
                    MemoEditorViewModel(
                        memoId = memoId,
                        fromMessageId = fromMessageId,
                        initialTodo = initialTodo,
                        memoDao = container.database.memoDao(),
                        messageDao = container.database.messageDao(),
                        quote = quote,
                        appScope = container.appScope,
                    )
                }
            }
    }
}
