package com.lorebookforge.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lorebookforge.app.AppViewModel
import com.lorebookforge.app.model.RecentFile
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: AppViewModel, onOpenBook: () -> Unit) {
    var showNewDialog by remember { mutableStateOf(false) }
    var pendingNewName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.refreshRecents() }

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && pendingNewName.isNotBlank()) {
            viewModel.createBook(uri, pendingNewName) { success ->
                if (success) onOpenBook()
            }
        }
    }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.openBook(uri) { success ->
                if (success) onOpenBook()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lorebook Forge") },
                actions = {
                    IconButton(onClick = { openLauncher.launch(arrayOf("application/json")) }) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "Open existing file")
                    }
                    IconButton(onClick = { showNewDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "New lorebook")
                    }
                }
            )
        }
    ) { padding ->
        if (viewModel.recentFiles.isEmpty()) {
            EmptyLibrary(Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(viewModel.recentFiles, key = { it.uri }) { recent ->
                    RecentFileRow(
                        recent = recent,
                        onClick = {
                            viewModel.openBook(Uri.parse(recent.uri)) { success ->
                                if (success) onOpenBook()
                            }
                        },
                        onRemove = { viewModel.removeRecent(recent.uri) }
                    )
                }
            }
        }
    }

    if (showNewDialog) {
        NewLorebookDialog(
            onDismiss = { showNewDialog = false },
            onConfirm = { name ->
                pendingNewName = name
                showNewDialog = false
                createLauncher.launch("${sanitizeFileName(name)}.json")
            }
        )
    }
}

private fun sanitizeFileName(name: String): String {
    val cleaned = name.trim().replace(Regex("[^A-Za-z0-9 _-]"), "").replace(" ", "_")
    return cleaned.ifEmpty { "lorebook" }
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Book, contentDescription = null, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("No lorebooks yet", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Tap + to create one, or open an existing lorebook JSON file from your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RecentFileRow(recent: RecentFile, onClick: () -> Unit, onRemove: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(recent.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(recent.lastOpened)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Remove from list") }, onClick = {
                        menuOpen = false
                        onRemove()
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewLorebookDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New lorebook") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
