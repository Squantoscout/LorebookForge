package com.lorebookforge.app.model

/** An entry in the library's "recently opened" list. This is also where
 *  Lorebook Forge keeps the handful of things about a book that aren't
 *  part of its plain JSON file at all: the name shown in the library, and
 *  which fields act as an entry's title and subtitle in the book screen. */
data class RecentFile(
    val uri: String,
    val name: String,
    val lastOpened: Long,
    val displayFieldId: String = "comment",
    val subtitleFieldId: String? = "key"
)
