package com.lorebookforge.app.model

/** An entry in the library's "previously opened" list. */
data class RecentFile(
    val uri: String,
    val name: String,
    val lastOpened: Long
)
