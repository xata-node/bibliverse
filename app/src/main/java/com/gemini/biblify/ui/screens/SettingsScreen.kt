// --- Файл: ui/screens/SettingsScreen.kt ---
package com.gemini.biblify.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gemini.biblify.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, navController: NavController) {
    val theme by viewModel.theme.collectAsState(initial = "dark")
    val notificationSettings by viewModel.notificationSettings.collectAsState(
        initial = com.gemini.biblify.data.DataStoreManager.NotificationSettings(false, 8, 0)
    )

    var notificationsEnabled by remember(notificationSettings.enabled) { mutableStateOf(notificationSettings.enabled) }
    var notificationHour by remember(notificationSettings.hour) { mutableStateOf(notificationSettings.hour) }
    var notificationMinute by remember(notificationSettings.minute) { mutableStateOf(notificationSettings.minute) }

    val context = LocalContext.current

    // FIX 1.4: Запрос разрешения на уведомления для Android 13+
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                notificationsEnabled = true
            }
        }
    )

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour: Int, minute: Int ->
            notificationHour = hour
            notificationMinute = minute
        }, notificationHour, notificationMinute, true
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.setNotifications(notificationsEnabled, notificationHour, notificationMinute)
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dark Mode")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = theme == "dark",
                    onCheckedChange = { viewModel.setTheme(it) }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Daily Verse Notification", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                notificationsEnabled = true
                            }
                        } else {
                            notificationsEnabled = false
                        }
                    }
                )
            }

            // FIX 1.1: Показываем диалог выбора времени
            if (notificationsEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { timePickerDialog.show() }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notification time", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "%02d:%02d".format(notificationHour, notificationMinute),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
