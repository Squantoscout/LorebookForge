package com.lorebookforge.app.data

import com.lorebookforge.app.model.AUTO_MANAGED_FIELD_IDS
import com.lorebookforge.app.model.Entry
import com.lorebookforge.app.model.SwitchDef
import com.lorebookforge.app.model.SwitchType
import com.lorebookforge.app.model.defaultSwitches
import com.lorebookforge.app.model.labelForFieldId
import org.json.JSONArray
import org.json.JSONObject

/** A parsed entry before it's wrapped into a live, observable [Entry]. */
data class ParsedEntry(val id: String, val values: Map<String, Any?>)

/** A whole lorebook file, freshly parsed from disk. There's no separate
 *  "name" or switch registry here - Lorebook Forge's own bookkeeping lives
 *  outside the file (see [RecentFilesStore]); what the switches even are
 *  is inferred from whichever fields the entries use. */
data class ParsedBook(
    val switches: List<SwitchDef>,
    val entries: List<ParsedEntry>
)

/**
 * Reads and writes a lorebook in the ordinary shape most lorebook tools
 * (e.g. SillyTavern) expect: a top-level "entries" object keyed by index,
 * with each entry's own fields - key, comment, content, and so on - sitting
 * directly on it, nothing else at the top level. Lorebook Forge's switches
 * are just whichever of those fields happen to be present; the switch
 * registry itself is never written to the file, so what comes out the
 * other end is a completely ordinary lorebook JSON, usable anywhere.
 */
object LorebookSerializer {

    fun toJson(switches: List<SwitchDef>, entries: List<Entry>): String {
        val entriesObj = JSONObject()
        entries.forEachIndexed { index, entry ->
            val o = JSONObject()
            // Written in switch order (not the underlying map's own
            // iteration order, which isn't guaranteed to match) so that
            // reordering switches in Manage Switches is also what reorders
            // the fields in the file.
            switches.forEach { sw ->
                if (entry.values.containsKey(sw.id)) {
                    putValue(o, sw.id, entry.values[sw.id], sw.type)
                }
            }
            // A field on the entry that isn't in the current switch
            // registry shouldn't normally happen, but is still written out
            // (after the known ones) rather than silently dropped.
            entry.values.forEach { (k, v) ->
                if (k !in AUTO_MANAGED_FIELD_IDS && switches.none { it.id == k }) {
                    putValue(o, k, v, null)
                }
            }
            // The app keeps these in lockstep with the entry's position,
            // rather than letting them drift from hand-editing or reorders.
            o.put("uid", index)
            o.put("displayIndex", index)
            entriesObj.put(index.toString(), o)
        }
        val root = JSONObject()
        root.put("entries", entriesObj)
        return root.toString(2)
    }

    private fun putValue(o: JSONObject, key: String, value: Any?, type: SwitchType?) {
        when {
            value == null -> o.put(key, JSONObject.NULL)
            type == SwitchType.TAGS && value is String -> {
                // A Tags switch is held internally as a comma-joined
                // string (so it edits the same way a Text field would);
                // written out as a real JSON array, same as any ordinary
                // lorebook's "key"/"keysecondary".
                val arr = JSONArray()
                value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { arr.put(it) }
                o.put(key, arr)
            }
            value is Boolean -> o.put(key, value)
            value is Double -> o.put(key, value)
            value is Int -> o.put(key, value)
            value is String -> o.put(key, value)
            else -> o.put(key, value.toString())
        }
    }

    fun fromJson(text: String): ParsedBook {
        // A file picked or created through the system file UI can briefly
        // be empty (zero bytes) before anything has been written to it.
        // Treat that as a fresh, empty book instead of a parse error.
        if (text.isBlank()) {
            return ParsedBook(defaultSwitches(), emptyList())
        }

        val root = JSONObject(text)
        val entriesNode = root.opt("entries")

        // Collect (object key, entry object) rows in file order, whether
        // "entries" is an object keyed by index (the ordinary shape) or,
        // just in case, a plain array.
        val rows = mutableListOf<Pair<String, JSONObject>>()
        when (entriesNode) {
            is JSONObject -> {
                val ids = mutableListOf<String>()
                val keyIter = entriesNode.keys()
                while (keyIter.hasNext()) ids.add(keyIter.next())
                // Numeric-looking keys sort numerically, so entries come
                // back in the order a lorebook tool would show them rather
                // than JSON object string-key order ("10" before "2").
                ids.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }.forEach { id ->
                    rows.add(id to entriesNode.getJSONObject(id))
                }
            }
            is JSONArray -> {
                for (i in 0 until entriesNode.length()) {
                    rows.add(i.toString() to entriesNode.getJSONObject(i))
                }
            }
            else -> {}
        }

        // Infer the switch registry from whichever fields the entries
        // actually use - the file carries no separate schema of its own.
        // A field's type comes from the first entry that has a non-null
        // value for it; a field that's null on every entry falls back to
        // Text. Order follows first appearance across the entries.
        val typeById = LinkedHashMap<String, SwitchType?>()
        rows.forEach { (_, o) ->
            val keyIter = o.keys()
            while (keyIter.hasNext()) {
                val k = keyIter.next()
                if (k in AUTO_MANAGED_FIELD_IDS) continue
                if (!typeById.containsKey(k)) typeById[k] = null
                if (typeById[k] == null) {
                    typeById[k] = inferType(o.get(k))
                }
            }
        }
        val switches = if (typeById.isEmpty()) {
            defaultSwitches()
        } else {
            typeById.map { (id, type) -> SwitchDef(id, labelForFieldId(id), type ?: SwitchType.TEXT) }
        }

        val entries = rows.map { (idxKey, o) ->
            val values = mutableMapOf<String, Any?>()
            val keyIter = o.keys()
            while (keyIter.hasNext()) {
                val k = keyIter.next()
                if (k in AUTO_MANAGED_FIELD_IDS) continue
                values[k] = readValue(o.get(k))
            }
            ParsedEntry(idxKey, values)
        }

        return ParsedBook(switches, entries)
    }

    private fun inferType(raw: Any?): SwitchType? = when (raw) {
        JSONObject.NULL, null -> null
        is Boolean -> SwitchType.TOGGLE
        is JSONArray -> SwitchType.TAGS
        is Int, is Long, is Double -> SwitchType.NUMBER
        is String -> SwitchType.TEXT
        else -> null
    }

    private fun readValue(raw: Any?): Any? = when (raw) {
        JSONObject.NULL -> null
        is Boolean -> raw
        is Int -> raw.toDouble()
        is Long -> raw.toDouble()
        is Double -> raw
        is String -> raw
        is JSONArray -> {
            val items = mutableListOf<String>()
            for (i in 0 until raw.length()) items.add(raw.get(i).toString())
            items.joinToString(", ")
        }
        else -> raw.toString()
    }
}
