// --- НОВЫЙ ФАЙЛ: ui/screens/AffirmationsScreen.kt ---
package com.gemini.bibliverse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.gemini.bibliverse.data.Verse
import com.gemini.bibliverse.viewmodel.MainViewModel

@Composable
private fun AddAffirmationDialog(
    onDismiss: () -> Unit,
    onAdd: (text: String, reference: String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Add New Affirmation", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Affirmation text") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Source (optional)") },
                    modifier = Modifier.fillMaxWidth()
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
                        onClick = { onAdd(text, reference.ifBlank { "(Affirmation)" }) },
                        enabled = text.isNotBlank()
                    ) {
                        Text("Add")
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
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddAffirmationDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { text, reference ->
                viewModel.addAffirmation(text, reference)
                showAddDialog = false
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
            FloatingActionButton(onClick = { showAddDialog = true }) {
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
                        onRemove = { viewModel.removeAffirmation(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun AffirmationItem(affirmation: Verse, onRemove: (Verse) -> Unit) {
    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(affirmation.text, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(affirmation.reference, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { onRemove(affirmation) }) {
                Icon(Icons.Default.Delete, contentDescription = "Remove Affirmation")
            }
        }
    }
}
