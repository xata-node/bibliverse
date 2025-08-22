package com.gemini.bibliverse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.gemini.bibliverse.data.Verse
import com.gemini.bibliverse.viewmodel.MainViewModel

@Composable
private fun AddOrEditAffirmationDialog(
    affirmationToEdit: Verse?, // Pass null for adding, existing verse for editing
    onDismiss: () -> Unit,
    onConfirm: (text: String, reference: String) -> Unit
) {
    var text by remember { mutableStateOf(affirmationToEdit?.text ?: "") }
    var reference by remember { mutableStateOf(affirmationToEdit?.reference ?: "") }
    val isEditing = affirmationToEdit != null

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (isEditing) "Edit Affirmation" else "Add New Affirmation",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Affirmation text") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = reference,
                    onValueChange = { newValue ->
                        // Filter out newlines to avoid issues with saving
                        reference = newValue.replace("\n", "").replace("\r", "")
                    },
                    label = { Text("Source (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true, // Disables multi-line input
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done // Changes the 'Enter' key to 'Done' explicitly
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        // Ensure the reference is trimmed of leading/trailing spaces
                        onClick = { onConfirm(text.trim(), reference.trim()) },
                        enabled = text.isNotBlank()
                    ) {
                        Text(if (isEditing) "Save" else "Add")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AffirmationsScreen(viewModel: MainViewModel, navController: NavController) {
    val affirmations by viewModel.affirmations.collectAsState()
    var affirmationToEdit by remember { mutableStateOf<Verse?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AddOrEditAffirmationDialog(
            affirmationToEdit = affirmationToEdit,
            onDismiss = {
                showDialog = false
                affirmationToEdit = null // Clear selection on dismiss
            },
            onConfirm = { text, reference ->
                if (affirmationToEdit != null) {
                    viewModel.editAffirmation(affirmationToEdit!!, text, reference)
                } else {
                    viewModel.addAffirmation(text, reference)
                }
                showDialog = false
                affirmationToEdit = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Affirmations") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                affirmationToEdit = null // Ensure we are in "add" mode
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Affirmation")
            }
        }
    ) { paddingValues ->
        if (affirmations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("You haven't added any affirmations yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(affirmations, key = { it.text + it.reference }) { affirmation ->
                    AffirmationItem(
                        affirmation = affirmation,
                        onEdit = {
                            affirmationToEdit = it
                            showDialog = true
                        },
                        onRemove = { viewModel.removeAffirmation(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun AffirmationItem(
    affirmation: Verse,
    onEdit: (Verse) -> Unit,
    onRemove: (Verse) -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(affirmation.text, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(4.dp))
                if(affirmation.reference.isNotBlank()) {
                    Text(affirmation.reference, style = MaterialTheme.typography.bodySmall)
                }
            }
            // Edit Button
            IconButton(onClick = { onEdit(affirmation) }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Affirmation")
            }
            // Remove Button
            IconButton(onClick = { onRemove(affirmation) }) {
                Icon(Icons.Default.Delete, contentDescription = "Remove Affirmation")
            }
        }
    }
}
