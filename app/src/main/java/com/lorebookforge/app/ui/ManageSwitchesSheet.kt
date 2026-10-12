package com.lorebookforge.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lorebookforge.app.AppViewModel
import com.lorebookforge.app.model.SwitchDef
import com.lorebookforge.app.model.SwitchType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSwitchesSheet(viewModel: AppViewModel, onDismiss: () -> Unit) {
    var showAdd by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text("Switches", style = MaterialTheme.typography.titleLarge)
            Text(
                "These are the properties every entry can have. Rename, retype, or remove any of them — including the defaults.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(viewModel.switches, key = { it.id }) { sw ->
                    SwitchEditRow(
                        switch = sw,
                        isDisplayField = viewModel.displayFieldId == sw.id,
                        isSubtitleField = viewModel.subtitleFieldId == sw.id,
                        onRename = { viewModel.renameSwitch(sw.id, it) },
                        onTypeChange = { viewModel.changeSwitchType(sw.id, it) },
                        onDelete = { viewModel.deleteSwitchGlobally(sw.id) },
                        onSetDisplay = { viewModel.setDisplayField(sw.id) },
                        onSetSubtitle = {
                            viewModel.setSubtitleField(if (viewModel.subtitleFieldId == sw.id) null else sw.id)
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add switch")
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showAdd) {
        AddSwitchDialog(
            onDismiss = { showAdd = false },
            onConfirm = { label, type ->
                viewModel.addNewSwitch(label, type)
                showAdd = false
            }
        )
    }
}

@Composable
private fun SwitchEditRow(
    switch: SwitchDef,
    isDisplayField: Boolean,
    isSubtitleField: Boolean,
    onRename: (String) -> Unit,
    onTypeChange: (SwitchType) -> Unit,
    onDelete: () -> Unit,
    onSetDisplay: () -> Unit,
    onSetSubtitle: () -> Unit
) {
    var label by remember(switch.id) { mutableStateOf(switch.label) }
    var typeMenuOpen by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = label,
                onValueChange = {
                    label = it
                    onRename(it)
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.width(8.dp))
            Box {
                AssistChip(
                    onClick = { typeMenuOpen = true },
                    label = { Text(switch.type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
                DropdownMenu(expanded = typeMenuOpen, onDismissRequest = { typeMenuOpen = false }) {
                    SwitchType.values().forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                typeMenuOpen = false
                                onTypeChange(t)
                            }
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete switch")
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = isDisplayField, onClick = onSetDisplay, label = { Text("Entry title") })
            Spacer(Modifier.width(6.dp))
            FilterChip(selected = isSubtitleField, onClick = onSetSubtitle, label = { Text("Entry subtitle") })
        }
        Divider(Modifier.padding(top = 8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSwitchDialog(onDismiss: () -> Unit, onConfirm: (String, SwitchType) -> Unit) {
    var label by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(SwitchType.TEXT) }
    var typeMenuOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add switch") },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                Box {
                    OutlinedButton(onClick = { typeMenuOpen = true }) {
                        Text("Type: " + type.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                    DropdownMenu(expanded = typeMenuOpen, onDismissRequest = { typeMenuOpen = false }) {
                        SwitchType.values().forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    type = t
                                    typeMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (label.isNotBlank()) onConfirm(label, type) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
