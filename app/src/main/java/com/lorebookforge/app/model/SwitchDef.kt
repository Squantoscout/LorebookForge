package com.lorebookforge.app.model

/** The kind of value a switch holds. */
enum class SwitchType { TEXT, NUMBER, TOGGLE }

/**
 * A single named property an entry can carry — e.g. "key", "content",
 * or something the person invented for their own proxy. The [id] is the
 * exact name written into the exported JSON; [label] is only shown in
 * the editor UI.
 */
data class SwitchDef(
    val id: String,
    val label: String,
    val type: SwitchType
)
