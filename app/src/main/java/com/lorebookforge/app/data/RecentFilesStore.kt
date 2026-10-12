package com.lorebookforge.app.data

import android.content.Context
import com.lorebookforge.app.model.RecentFile
import org.json.JSONArray
import org.json.JSONObject

/**
 * The library's "previously opened" list. Stored as a small JSON blob in
 * SharedPreferences — just bookkeeping (uri, name, last-opened time), not
 * the lorebook data itself, which always lives in the file the person
 * picked.
 */
object RecentFilesStore {
    private const val PREFS = "lorebook_forge_prefs"
    private const val KEY_RECENTS = "recent_files"
    private const val MAX_RECENTS = 30

    fun load(context: Context): List<RecentFile> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_RECENTS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<RecentFile>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(RecentFile(o.getString("uri"), o.getString("name"), o.optLong("lastOpened", 0L)))
            }
            list.sortedByDescending { it.lastOpened }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun touch(context: Context, uri: String, name: String) {
        val current = load(context).toMutableList()
        current.removeAll { it.uri == uri }
        current.add(0, RecentFile(uri, name, System.currentTimeMillis()))
        save(context, current.take(MAX_RECENTS))
    }

    fun remove(context: Context, uri: String) {
        val current = load(context).toMutableList()
        current.removeAll { it.uri == uri }
        save(context, current)
    }

    private fun save(context: Context, list: List<RecentFile>) {
        val arr = JSONArray()
        list.forEach {
            val o = JSONObject()
            o.put("uri", it.uri)
            o.put("name", it.name)
            o.put("lastOpened", it.lastOpened)
            arr.put(o)
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_RECENTS, arr.toString()).apply()
    }
}
