package com.lorebookforge.app.model

/** The switch set a brand-new lorebook starts with - matching the field
 *  set an ordinary lorebook / World Info entry uses in tools like
 *  SillyTavern, so a book made here opens cleanly elsewhere. Every one of
 *  these can be freely renamed, retyped, or deleted afterward - nothing
 *  here is fixed. ("uid" and "displayIndex" aren't included: the app
 *  manages those on its own, see [AUTO_MANAGED_FIELD_IDS].) */
fun defaultSwitches(): List<SwitchDef> = listOf(
    SwitchDef("key", "Primary Keys", SwitchType.TAGS),
    SwitchDef("keysecondary", "Secondary Keys", SwitchType.TAGS),
    SwitchDef("comment", "Title / Comment", SwitchType.TEXT),
    SwitchDef("content", "Content", SwitchType.TEXT),
    SwitchDef("constant", "Constant", SwitchType.TOGGLE),
    SwitchDef("vectorized", "Vectorized", SwitchType.TOGGLE),
    SwitchDef("selective", "Selective", SwitchType.TOGGLE),
    SwitchDef("selectiveLogic", "Selective Logic", SwitchType.NUMBER),
    SwitchDef("addMemo", "Add Memo", SwitchType.TOGGLE),
    SwitchDef("order", "Insertion Order", SwitchType.NUMBER),
    SwitchDef("position", "Position", SwitchType.NUMBER),
    SwitchDef("disable", "Disabled", SwitchType.TOGGLE),
    SwitchDef("excludeRecursion", "Exclude Recursion", SwitchType.TOGGLE),
    SwitchDef("preventRecursion", "Prevent Recursion", SwitchType.TOGGLE),
    SwitchDef("delayUntilRecursion", "Delay Until Recursion", SwitchType.TOGGLE),
    SwitchDef("probability", "Probability", SwitchType.NUMBER),
    SwitchDef("useProbability", "Use Probability", SwitchType.TOGGLE),
    SwitchDef("depth", "Depth", SwitchType.NUMBER),
    SwitchDef("group", "Group", SwitchType.TEXT),
    SwitchDef("groupOverride", "Group Override", SwitchType.TOGGLE),
    SwitchDef("groupWeight", "Group Weight", SwitchType.NUMBER),
    SwitchDef("scanDepth", "Scan Depth", SwitchType.NUMBER),
    SwitchDef("caseSensitive", "Case Sensitive", SwitchType.TOGGLE),
    SwitchDef("matchWholeWords", "Match Whole Words", SwitchType.TOGGLE),
    SwitchDef("useGroupScoring", "Use Group Scoring", SwitchType.TOGGLE),
    SwitchDef("automationId", "Automation ID", SwitchType.TEXT),
    SwitchDef("role", "Role", SwitchType.NUMBER),
    SwitchDef("sticky", "Sticky", SwitchType.NUMBER),
    SwitchDef("cooldown", "Cooldown", SwitchType.NUMBER),
    SwitchDef("delay", "Delay", SwitchType.NUMBER)
)

/** Field ids the app manages itself (always equal to the entry's position)
 *  and never shows as an editable switch. */
val AUTO_MANAGED_FIELD_IDS = setOf("uid", "displayIndex")

/** A friendly label for a field id - used when opening a file whose fields
 *  don't exactly match [defaultSwitches] (an existing or hand-made
 *  lorebook, say). Falls back to turning camelCase/snake_case into Title
 *  Case so an unrecognized field still gets something readable. */
fun labelForFieldId(id: String): String {
    defaultSwitches().firstOrNull { it.id == id }?.let { return it.label }
    val spaced = id.replace(Regex("([a-z0-9])([A-Z])"), "$1 $2").replace('_', ' ')
    return spaced.split(" ").filter { it.isNotBlank() }
        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
}

fun defaultValueFor(type: SwitchType, switchId: String): Any? = when (type) {
    SwitchType.TEXT -> ""
    SwitchType.TAGS -> ""
    SwitchType.NUMBER -> 0.0
    SwitchType.TOGGLE -> false
}

fun defaultValuesForSwitches(switches: List<SwitchDef>): MutableMap<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    switches.forEach { map[it.id] = defaultValueFor(it.type, it.id) }
    return map
}
