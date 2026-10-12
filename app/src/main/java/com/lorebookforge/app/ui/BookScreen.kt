package com.lorebookforge.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lorebookforge.app.AppViewModel
import com.lorebookforge.app.model.Entry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookScreen(viewModel: AppViewModel, onBack: () -> Unit, onOpenEntry: (String) -> Unit) {
    var showManageSwitches by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.bookName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back to library")
                    }
                },
                actions = {
                    IconButton(onClick = { showManageSwitches = true }) {
                        Icon(Icons.Filled.Tune, contentDescription = "Manage switches")
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Rename lorebook") }, onClick = {
                                menuOpen = false
                                showRename = true
                            })
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val id = viewModel.addEntry()
                    onOpenEntry(id)
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New entry") }
            )
        }
    ) { padding ->
        if (viewModel.entries.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No entries yet", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tap \"New entry\" to add your first piece of lore.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp)
            ) {
                items(viewModel.entries, key = { it.id }) { entry ->
                    EntryRow(
                        entry = entry,
                        displayFieldId = viewModel.displayFieldId,
                        subtitleFieldId = viewModel.subtitleFieldId,
                        onClick = { onOpenEntry(entry.id) }
                    )
                }
            }
        }
    }

    if (showManageSwitches) {
        ManageSwitchesSheet(viewModel = viewModel, onDismiss = { showManageSwitches = false })
    }

    if (showRename) {
        RenameDialog(
            current = viewModel.bookName,
            onDismiss = { showRename = false },
            onConfirm = {
                viewModel.renameBook(it)
                showRename = false
            }
        )
    }
}

@Composable
private fun EntryRow(entry: Entry, displayFieldId: String, subtitleFieldId: String?, onClick: () -> Unit) {
    val title = (entry.values[displayFieldId] as? String)?.takeIf { it.isNotBlank() } ?: "(untitled entry)"
    val subtitle = subtitleFieldId?.let { entry.values[it] as? String }?.takeIf { it.isNotBlank() }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenameDialog(current: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename lorebook") },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
