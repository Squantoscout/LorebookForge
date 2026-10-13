package com.lorebookforge.app

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lorebookforge.app.data.FileRepository
import com.lorebookforge.app.data.ParsedBook
import com.lorebookforge.app.data.RecentFilesStore
import com.lorebookforge.app.model.Entry
import com.lorebookforge.app.model.RecentFile
import com.lorebookforge.app.model.SwitchDef
import com.lorebookforge.app.model.SwitchType
import com.lorebookforge.app.model.defaultSwitches
import com.lorebookforge.app.model.defaultValueFor
import com.lorebookforge.app.model.defaultValuesForSwitches
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class AppViewModel(app: Application) : AndroidViewModel(app) {

    // ---- library ----
    var recentFiles by mutableStateOf<List<RecentFile>>(emptyList())
        private set

    // ---- current book ----
    // bookName/displayFieldId/subtitleFieldId are Lorebook Forge's own
    // bookkeeping - none of it lives in the JSON file itself (see
    // LorebookSerializer), so it's persisted in RecentFilesStore instead.
    var bookName by mutableStateOf("")
        private set
    var displayFieldId by mutableStateOf("comment")
        private set
    var subtitleFieldId by mutableStateOf<String?>("key")
        private set
    var currentUri by mutableStateOf<Uri?>(null)
        private set

    val switches = mutableStateListOf<SwitchDef>()
    val entries = mutableStateListOf<Entry>()

    // ---- current entry ----
    var currentEntryId by mutableStateOf<String?>(null)
        private set

    // ---- errors, surfaced as a snackbar instead of crashing ----
    var lastError by mutableStateOf<String?>(null)
        private set

    fun clearError() {
        lastError = null
    }

    private var saveJob: Job? = null

    fun refreshRecents() {
        recentFiles = RecentFilesStore.load(getApplication())
    }

    /** Opens a book from disk. [onDone] fires with true on success, false on
     *  failure (in which case [lastError] is set and nothing navigates). */
    fun openBook(uri: Uri, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                grantPersistablePermission(uri)
                val parsed = withContext(Dispatchers.IO) { FileRepository.read(context, uri) }

                // The file itself carries none of this - recover it from
                // the library's own bookkeeping if we've opened this file
                // before, otherwise make a sensible guess.
                val existing = RecentFilesStore.load(context).find { it.uri == uri.toString() }
                val (inferredDisplay, inferredSubtitle) = inferDisplayFields(parsed.switches)
                val resolvedName = existing?.name ?: deriveNameFromUri(context, uri)
                val resolvedDisplay = existing?.displayFieldId ?: inferredDisplay
                val resolvedSubtitle = if (existing != null) existing.subtitleFieldId else inferredSubtitle

                loadParsedIntoState(parsed, resolvedName, resolvedDisplay, resolvedSubtitle)
                currentUri = uri
                RecentFilesStore.touch(context, uri.toString(), resolvedName, resolvedDisplay, resolvedSubtitle)
                refreshRecents()
                onDone(true)
            } catch (e: Exception) {
                lastError = "Couldn't open that file: ${describeError(e)}"
                onDone(false)
            }
        }
    }

    /** Creates a brand-new book at [uri]. [onDone] fires with true on
     *  success, false on failure (in which case [lastError] is set). */
    fun createBook(uri: Uri, name: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                grantPersistablePermission(uri)
                bookName = name
                displayFieldId = "comment"
                subtitleFieldId = "key"
                switches.clear()
                switches.addAll(defaultSwitches())
                entries.clear()
                currentUri = uri
                withContext(Dispatchers.IO) {
                    FileRepository.write(context, uri, switches.toList(), entries.toList())
                }
                RecentFilesStore.touch(context, uri.toString(), name, displayFieldId, subtitleFieldId)
                refreshRecents()
                onDone(true)
            } catch (e: Exception) {
                lastError = "Couldn't create that file: ${describeError(e)}"
                currentUri = null
                onDone(false)
            }
        }
    }

    private fun loadParsedIntoState(
        parsed: ParsedBook,
        name: String,
        displayField: String,
        subtitleField: String?
    ) {
        bookName = name
        displayFieldId = displayField
        subtitleFieldId = subtitleField
        switches.clear()
        switches.addAll(parsed.switches)
        entries.clear()
        parsed.entries.forEach { pe ->
            val map = mutableStateMapOf<String, Any?>()
            map.putAll(pe.values)
            entries.add(Entry(pe.id, map))
        }
    }

    fun closeBook() {
        persistNow()
        currentUri = null
        currentEntryId = null
        switches.clear()
        entries.clear()
        bookName = ""
    }

    fun removeRecent(uri: String) {
        RecentFilesStore.remove(getApplication(), uri)
        refreshRecents()
    }

    fun openEntry(id: String) { currentEntryId = id }
    fun closeEntry() { currentEntryId = null }

    fun entryById(id: String): Entry? = entries.find { it.id == id }

    fun renameBook(newName: String) {
        bookName = newName
        scheduleSave()
    }

    fun addEntry(): String {
        val id = UUID.randomUUID().toString().take(8)
        val map = mutableStateMapOf<String, Any?>()
        map.putAll(defaultValuesForSwitches(switches))
        entries.add(Entry(id, map))
        scheduleSave()
        return id
    }

    fun deleteEntry(id: String) {
        entries.removeAll { it.id == id }
        scheduleSave()
    }

    fun updateEntryValue(entryId: String, switchId: String, value: Any?) {
        entryById(entryId)?.values?.set(switchId, value)
        scheduleSave()
    }

    fun removeSwitchFromEntry(entryId: String, switchId: String) {
        entryById(entryId)?.values?.remove(switchId)
        scheduleSave()
    }

    fun addSwitchToEntry(entryId: String, switchId: String) {
        val entry = entryById(entryId) ?: return
        val def = switches.find { it.id == switchId } ?: return
        if (!entry.values.containsKey(switchId)) {
            entry.values[switchId] = defaultValueFor(def.type, def.id)
        }
        scheduleSave()
    }

    /** Adds a brand-new switch to the book's registry, optionally turning
     *  it on for one entry right away, and returns its generated id. */
    fun addNewSwitch(label: String, type: SwitchType, addToEntryId: String? = null): String {
        var id = slugify(label)
        if (id.isEmpty()) id = "switch"
        var unique = id
        var n = 1
        while (switches.any { it.id == unique }) {
            n++
            unique = "$id$n"
        }
        switches.add(SwitchDef(unique, label, type))
        if (addToEntryId != null) {
            addSwitchToEntry(addToEntryId, unique)
        } else {
            scheduleSave()
        }
        return unique
    }

    /** Moves a switch from one position in the registry to another. Switch
     *  order is also field order in the file, so this is both a display
     *  preference and, on the next save, the order the JSON is written in. */
    fun moveSwitch(from: Int, to: Int) {
        if (from == to || from !in switches.indices || to !in switches.indices) return
        val item = switches.removeAt(from)
        switches.add(to, item)
        scheduleSave()
    }

    fun renameSwitch(switchId: String, newLabel: String) {
        val idx = switches.indexOfFirst { it.id == switchId }
        if (idx == -1) return
        switches[idx] = switches[idx].copy(label = newLabel)
        scheduleSave()
    }

    fun changeSwitchType(switchId: String, newType: SwitchType) {
        val idx = switches.indexOfFirst { it.id == switchId }
        if (idx == -1) return
        switches[idx] = switches[idx].copy(type = newType)
        entries.forEach { entry ->
            if (entry.values.containsKey(switchId)) {
                entry.values[switchId] = defaultValueFor(newType, switchId)
            }
        }
        scheduleSave()
    }

    fun deleteSwitchGlobally(switchId: String) {
        switches.removeAll { it.id == switchId }
        entries.forEach { it.values.remove(switchId) }
        if (displayFieldId == switchId) {
            displayFieldId = switches.firstOrNull {
                it.type == SwitchType.TEXT || it.type == SwitchType.TAGS
            }?.id ?: ""
        }
        if (subtitleFieldId == switchId) {
            subtitleFieldId = switches.firstOrNull {
                (it.type == SwitchType.TEXT || it.type == SwitchType.TAGS) && it.id != displayFieldId
            }?.id
        }
        scheduleSave()
    }

    fun setDisplayField(id: String) {
        displayFieldId = id
        scheduleSave()
    }

    fun setSubtitleField(id: String?) {
        subtitleFieldId = id
        scheduleSave()
    }

    /** A sensible guess at title/subtitle fields for a file we have no
     *  prior bookkeeping for: "comment"/"key" if present, otherwise the
     *  first text-ish switches available. */
    private fun inferDisplayFields(fromSwitches: List<SwitchDef>): Pair<String, String?> {
        val textish = fromSwitches.filter { it.type == SwitchType.TEXT || it.type == SwitchType.TAGS }
        val display = textish.firstOrNull { it.id == "comment" }?.id
            ?: textish.firstOrNull()?.id
            ?: fromSwitches.firstOrNull()?.id
            ?: ""
        val subtitle = textish.firstOrNull { it.id == "key" && it.id != display }?.id
            ?: textish.firstOrNull { it.id != display }?.id
        return display to subtitle
    }

    private fun deriveNameFromUri(context: Context, uri: Uri): String {
        return FileRepository.displayName(context, uri).removeSuffix(".json")
    }

    private fun grantPersistablePermission(uri: Uri) {
        try {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            // Permission may already be held, or the provider doesn't grant
            // persistable access; either way, carry on with what we have.
        }
    }

    private fun slugify(s: String): String {
        return s.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
    }

    private fun describeError(e: Exception): String {
        return e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400)
            persistNow()
        }
    }

    /** Fire-and-forget autosave: snapshots current state on the calling
     *  thread, syncs the library's bookkeeping (name, title/subtitle
     *  fields - none of which live in the JSON file), then writes the
     *  book off the main thread. Failures are surfaced via [lastError]
     *  rather than crashing the editor. */
    fun persistNow() {
        val uri = currentUri ?: return
        val name = bookName
        val sw = switches.toList()
        val en = entries.toList()
        val df = displayFieldId
        val sf = subtitleFieldId
        RecentFilesStore.touch(getApplication(), uri.toString(), name, df, sf)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FileRepository.write(getApplication(), uri, sw, en)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    lastError = "Couldn't save: ${describeError(e)}"
                }
            }
        }
    }
}
