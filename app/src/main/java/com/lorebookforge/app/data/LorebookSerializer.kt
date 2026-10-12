package com.lorebookforge.app.data

import com.lorebookforge.app.model.Entry
import com.lorebookforge.app.model.SwitchDef
import com.lorebookforge.app.model.SwitchType
import org.json.JSONArray
import org.json.JSONObject

/** A parsed entry before it's wrapped into a live, observable [Entry]. */
data class ParsedEntry(val id: String, val values: Map<String, Any?>)

/** A whole lorebook file, freshly parsed from disk. */
data class ParsedBook(
    val name: String,
    val switches: List<SwitchDef>,
    val entries: List<ParsedEntry>,
    val displayFieldId: String,
    val subtitleFieldId: String?
)

/**
 * Reads and writes the lorebook file format: every switch a book uses is
 * declared by name and type in a top-level "switches" array, so the file
 * is self-describing — it carries its own schema instead of assuming a
 * fixed one, which is what lets switches be freely renamed per book.
 */
object LorebookSerializer {

    fun toJson(
        name: String,
        switches: List<SwitchDef>,
        entries: List<Entry>,
        displayFieldId: String,
        subtitleFieldId: String?
    ): String {
        val root = JSONObject()
        root.put("name", name)
        root.put("displayFieldId", displayFieldId)
        root.put("subtitleFieldId", subtitleFieldId ?: JSONObject.NULL)

        val switchesArr = JSONArray()
        switches.forEach { sw ->
            val o = JSONObject()
            o.put("id", sw.id)
            o.put("label", sw.label)
            o.put("type", sw.type.name.lowercase())
            switchesArr.put(o)
        }
        root.put("switches", switchesArr)

        val entriesArr = JSONArray()
        entries.forEach { entry ->
            val o = JSONObject()
            o.put("id", entry.id)
            val valuesObj = JSONObject()
            entry.values.forEach { (k, v) ->
                when (v) {
                    null -> valuesObj.put(k, JSONObject.NULL)
                    is Boolean -> valuesObj.put(k, v)
                    is Double -> valuesObj.put(k, v)
                    is Int -> valuesObj.put(k, v)
                    is String -> valuesObj.put(k, v)
                    else -> valuesObj.put(k, v.toString())
                }
            }
            o.put("values", valuesObj)
            entriesArr.put(o)
        }
        root.put("entries", entriesArr)

        return root.toString(2)
    }

    fun fromJson(text: String): ParsedBook {
        val root = JSONObject(text)
        val name = root.optString("name", "Untitled Lorebook")

        val switches = mutableListOf<SwitchDef>()
        val switchesArr = root.optJSONArray("switches") ?: JSONArray()
        for (i in 0 until switchesArr.length()) {
            val o = switchesArr.getJSONObject(i)
            val type = when (o.optString("type", "text")) {
                "toggle" -> SwitchType.TOGGLE
                "number" -> SwitchType.NUMBER
                else -> SwitchType.TEXT
            }
            switches.add(SwitchDef(o.getString("id"), o.optString("label", o.getString("id")), type))
        }

        val entries = mutableListOf<ParsedEntry>()
        val entriesArr = root.optJSONArray("entries") ?: JSONArray()
        for (i in 0 until entriesArr.length()) {
            val o = entriesArr.getJSONObject(i)
            val id = o.optString("id", i.toString())
            val values = mutableMapOf<String, Any?>()
            val valuesObj = o.optJSONObject("values") ?: JSONObject()
            val keyIter = valuesObj.keys()
            while (keyIter.hasNext()) {
                val k = keyIter.next()
                val raw = valuesObj.get(k)
                values[k] = when (raw) {
                    JSONObject.NULL -> null
                    is Boolean -> raw
                    is Int -> raw.toDouble()
                    is Double -> raw
                    is String -> raw
                    else -> raw.toString()
                }
            }
            entries.add(ParsedEntry(id, values))
        }

        val displayFieldId = root.optString("displayFieldId", "comment")
        val subtitleFieldId = if (root.isNull("subtitleFieldId")) {
            null
        } else {
            root.optString("subtitleFieldId", "key").ifEmpty { null }
        }

        return ParsedBook(name, switches, entries, displayFieldId, subtitleFieldId)
    }
}
