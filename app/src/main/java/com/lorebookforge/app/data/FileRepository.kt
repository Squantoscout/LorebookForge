package com.lorebookforge.app.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.lorebookforge.app.model.Entry
import com.lorebookforge.app.model.SwitchDef

/**
 * Reads and writes a lorebook file directly through the Storage Access
 * Framework. No network, no app-private copy: the content Uri the person
 * picked is the file, read and written in place.
 */
object FileRepository {

    fun read(context: Context, uri: Uri): ParsedBook {
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().readText()
        } ?: throw IllegalStateException("Could not open file for reading")
        return LorebookSerializer.fromJson(text)
    }

    fun write(
        context: Context,
        uri: Uri,
        switches: List<SwitchDef>,
        entries: List<Entry>
    ) {
        val json = LorebookSerializer.toJson(switches, entries)
        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.write(json.toByteArray())
        } ?: throw IllegalStateException("Could not open file for writing")
    }

    fun displayName(context: Context, uri: Uri): String {
        return try {
            DocumentFile.fromSingleUri(context, uri)?.name ?: "Lorebook"
        } catch (e: Exception) {
            "Lorebook"
        }
    }
}
