package com.lorebookforge.app.model

/** The switch set a brand-new lorebook starts with. Every one of these can
 *  be freely renamed, retyped, or deleted afterward — nothing here is fixed. */
fun defaultSwitches(): List<SwitchDef> = listOf(
    SwitchDef("comment", "Title / Comment", SwitchType.TEXT),
    SwitchDef("key", "Primary Keys", SwitchType.TEXT),
    SwitchDef("keysecondary", "Secondary Keys", SwitchType.TEXT),
    SwitchDef("content", "Content", SwitchType.TEXT),
    SwitchDef("enabled", "Enabled", SwitchType.TOGGLE),
    SwitchDef("constant", "Constant", SwitchType.TOGGLE),
    SwitchDef("selective", "Selective", SwitchType.TOGGLE),
    SwitchDef("caseSensitive", "Case Sensitive", SwitchType.TOGGLE),
    SwitchDef("excludeRecursion", "Exclude Recursion", SwitchType.TOGGLE),
    SwitchDef("order", "Insertion Order", SwitchType.NUMBER),
    SwitchDef("probability", "Probability", SwitchType.NUMBER)
)

fun defaultValueFor(type: SwitchType, switchId: String): Any? = when (type) {
    SwitchType.TEXT -> ""
    SwitchType.NUMBER -> 0.0
    SwitchType.TOGGLE -> switchId == "enabled"
}

fun defaultValuesForSwitches(switches: List<SwitchDef>): MutableMap<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    switches.forEach { map[it.id] = defaultValueFor(it.type, it.id) }
    return map
}
