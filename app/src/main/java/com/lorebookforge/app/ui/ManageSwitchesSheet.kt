package com.lorebookforge.app.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.lorebookforge.app.AppViewModel
import com.lorebookforge.app.model.SwitchDef
import com.lorebookforge.app.model.SwitchType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSwitchesSheet(viewModel: AppViewModel, onDismiss: () -> Unit) {
    var showAdd by remember { mutableStateOf(false) }

    // Drag-to-reorder state. Rows aren't a fixed height (the text field,
    // chips, etc. vary a little), so each row reports its own measured
    // height here rather than assuming one.
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableStateOf(0f) }
    val rowHeights = remember { mutableStateMapOf<String, Int>() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text("Switches", style = MaterialTheme.typography.titleLarge)
            Text(
                "These are the properties every entry can have. Rename, retype, remove, or drag the handle to reorder them - including the defaults. Their order here is also the order they're written in the file.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                itemsIndexed(viewModel.switches, key = { _, sw -> sw.id }) { _, sw ->
                    val isDragging = draggingId == sw.id
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
                        },
                        rowModifier = Modifier
                            .onGloballyPositioned { coords ->
                                rowHeights[sw.id] = coords.size.height
                            }
                            .graphicsLayer {
                                translationY = if (isDragging) dragOffsetPx else 0f
                            }
                            .zIndex(if (isDragging) 1f else 0f),
                        dragHandleModifier = Modifier.pointerInput(sw.id) {
                            detectDragGestures(
                                onDragStart = {
                                    draggingId = sw.id
                                    dragOffsetPx = 0f
                                },
                                onDragEnd = {
                                    draggingId = null
                                    dragOffsetPx = 0f
                                },
                                onDragCancel = {
                                    draggingId = null
                                    dragOffsetPx = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetPx += dragAmount.y
                                    val myHeight = rowHeights[sw.id] ?: return@detectDragGestures
                                    val currentIndex = viewModel.switches.indexOfFirst { it.id == sw.id }
                                    if (currentIndex == -1) return@detectDragGestures
                                    if (dragOffsetPx > myHeight / 2f && currentIndex < viewModel.switches.lastIndex) {
                                        val neighbor = viewModel.switches[currentIndex + 1]
                                        val neighborHeight = rowHeights[neighbor.id] ?: myHeight
                                        viewModel.moveSwitch(currentIndex, currentIndex + 1)
                                        dragOffsetPx -= neighborHeight
                                    } else if (dragOffsetPx < -myHeight / 2f && currentIndex > 0) {
                                        val neighbor = viewModel.switches[currentIndex - 1]
                                        val neighborHeight = rowHeights[neighbor.id] ?: myHeight
                                        viewModel.moveSwitch(currentIndex, currentIndex - 1)
                                        dragOffsetPx += neighborHeight
                                    }
                                }
                            )
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
    onSetSubtitle: () -> Unit,
    rowModifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier
) {
    var label by remember(switch.id) { mutableStateOf(switch.label) }
    var typeMenuOpen by remember { mutableStateOf(false) }

    Column(
        rowModifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = dragHandleModifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.DragHandle, contentDescription = "Drag to reorder \"${switch.label}\"")
            }
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
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 40.dp)) {
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
