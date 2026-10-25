package com.example.starbox.capture

/** The last notification the reader had to skip because reading it failed (shown in Settings). */
data class ReaderError(val at: Long, val summary: String) {
    companion object {
        private const val MAX_MESSAGE = 160

        /** "NullPointerException: … (Person.fromBundle)": enough to tell what broke where. */
        fun from(error: Throwable, at: Long = System.currentTimeMillis()): ReaderError {
            val message = error.message?.take(MAX_MESSAGE)?.let { ": $it" }.orEmpty()
            val frame = error.stackTrace.firstOrNull()
                ?.let { " (${it.className.substringAfterLast('.')}.${it.methodName})" }
                .orEmpty()
            return ReaderError(at, error.javaClass.simpleName + message + frame)
        }
    }
}
