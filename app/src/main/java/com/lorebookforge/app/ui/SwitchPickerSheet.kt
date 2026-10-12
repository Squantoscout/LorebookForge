package com.lorebookforge.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lorebookforge.app.AppViewModel

/** The "+" toast in the entry editor: pick an existing switch to turn
 *  back on for this entry, or define a brand-new one from the bottom. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitchPickerSheet(viewModel: AppViewModel, entryId: String, onDismiss: () -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    val entry = viewModel.entryById(entryId)
    val available = viewModel.switches.filter { entry?.values?.containsKey(it.id) != true }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text("Add a switch to this entry", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (available.isEmpty()) {
                Text(
                    "Every existing switch is already on this entry.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(available, key = { it.id }) { sw ->
                        ListItem(
                            headlineContent = { Text(sw.label) },
                            supportingContent = { Text(sw.type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.clickable {
                                viewModel.addSwitchToEntry(entryId, sw.id)
                                onDismiss()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("New switch")
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showAdd) {
        AddSwitchDialog(
            onDismiss = { showAdd = false },
            onConfirm = { label, type ->
                viewModel.addNewSwitch(label, type, addToEntryId = entryId)
                showAdd = false
                onDismiss()
            }
        )
    }
}
