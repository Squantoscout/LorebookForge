package com.lorebookforge.app.model

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap

/**
 * One lorebook entry. [values] maps a switch id to whatever that switch
 * currently holds for this entry (String, Double, or Boolean). A switch
 * absent from this map simply isn't "on" for this entry.
 *
 * Backed by a SnapshotStateMap so Compose observes puts/removes directly,
 * without needing to replace the whole Entry or its parent list.
 */
class Entry(
    val id: String,
    val values: SnapshotStateMap<String, Any?> = mutableStateMapOf()
)
