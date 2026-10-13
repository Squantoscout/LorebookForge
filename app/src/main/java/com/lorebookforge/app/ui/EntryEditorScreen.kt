package com.lorebookforge.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lorebookforge.app.AppViewModel
import com.lorebookforge.app.model.SwitchType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditorScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val entryId = viewModel.currentEntryId
    var showPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (entryId == null) {
        onBack()
        return
    }
    val entry = viewModel.entryById(entryId)
    if (entry == null) {
        onBack()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entry") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete entry")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showPicker = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add switch")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            viewModel.switches.forEach { sw ->
                if (entry.values.containsKey(sw.id)) {
                    SwitchFieldRow(
                        label = sw.label,
                        type = sw.type,
                        value = entry.values[sw.id],
                        onValueChange = { viewModel.updateEntryValue(entryId, sw.id, it) },
                        onRemove = { viewModel.removeSwitchFromEntry(entryId, sw.id) }
                    )
                    Spacer(Modifier.height(14.dp))
                }
            }
            Spacer(Modifier.height(72.dp))
        }
    }

    if (showPicker) {
        SwitchPickerSheet(
            viewModel = viewModel,
            entryId = entryId,
            onDismiss = { showPicker = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this entry?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteEntry(entryId)
                    onBack()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SwitchFieldRow(
    label: String,
    type: SwitchType,
    value: Any?,
    onValueChange: (Any?) -> Unit,
    onRemove: () -> Unit
) {
    when (type) {
        SwitchType.TOGGLE -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = value as? Boolean ?: false, onCheckedChange = { onValueChange(it) })
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove $label from this entry")
                    }
                }
            }
        }
        SwitchType.NUMBER -> {
            Column {
                FieldHeader(label, onRemove)
                OutlinedTextField(
                    value = numberToText(value),
                    onValueChange = { text -> onValueChange(text.toDoubleOrNull() ?: 0.0) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
        SwitchType.TEXT -> {
            Column {
                FieldHeader(label, onRemove)
                OutlinedTextField(
                    value = value as? String ?: "",
                    onValueChange = { onValueChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = if (label.equals("content", ignoreCase = true)) 4 else 1
                )
            }
        }
        SwitchType.TAGS -> {
            TagsFieldRow(label = label, value = value, onValueChange = onValueChange, onRemove = onRemove)
        }
    }
}

@Composable
private fun FieldHeader(label: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = "Remove $label from this entry")
        }
    }
}

/** A switch whose value is a short list of words or phrases - entry keys,
 *  typically. Stored as the same comma-joined string a TEXT switch would
 *  use (so the file format is unchanged), but edited one item at a time:
 *  type a word, press Enter or type a comma, and it becomes a chip you can
 *  tap to remove instead of punctuation you have to get right. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsFieldRow(
    label: String,
    value: Any?,
    onValueChange: (Any?) -> Unit,
    onRemove: () -> Unit
) {
    val initial = (value as? String ?: "")
    val tags = remember(key1 = Unit) {
        mutableStateListOf<String>().apply {
            addAll(initial.split(",").map { it.trim() }.filter { it.isNotEmpty() })
        }
    }
    var draft by remember { mutableStateOf("") }

    fun syncOut() {
        onValueChange(tags.joinToString(", "))
    }

    fun commitDraft() {
        val t = draft.trim()
        draft = ""
        if (t.isNotEmpty() && !tags.contains(t)) {
            tags.add(t)
            syncOut()
        }
    }

    Column {
        FieldHeader(label, onRemove)
        if (tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = {
                            tags.remove(tag)
                            syncOut()
                        },
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove \"$tag\"",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }
        OutlinedTextField(
            value = draft,
            onValueChange = { text ->
                // Typing a comma commits the word before it as a chip,
                // same as pressing Enter.
                if (text.contains(",")) {
                    val parts = text.split(",")
                    parts.dropLast(1).forEach { part ->
                        val t = part.trim()
                        if (t.isNotEmpty() && !tags.contains(t)) tags.add(t)
                    }
                    draft = parts.last()
                    syncOut()
                } else {
                    draft = text
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Type a word, then Enter or a comma") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commitDraft() })
        )
    }
}

private fun numberToText(value: Any?): String {
    return when (value) {
        is Double -> if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
        is Int -> value.toString()
        else -> "0"
    }
}
