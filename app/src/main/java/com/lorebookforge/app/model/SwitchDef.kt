package com.lorebookforge.app.model

/** The kind of value a switch holds. [TAGS] is a list of short words or
 *  phrases (like entry keys) entered one at a time as removable chips,
 *  rather than typed out with commas - stored as the same comma-joined
 *  string a [TEXT] switch would use, so the file format doesn't change. */
enum class SwitchType { TEXT, NUMBER, TOGGLE, TAGS }

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
