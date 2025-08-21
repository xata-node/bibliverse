package com.gemini.bibliverse.ui.screens

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.android.billingclient.api.ProductDetails
import com.gemini.bibliverse.ui.navigation.Screen
import com.gemini.bibliverse.viewmodel.MainViewModel

@Composable
private fun DonationDialog(
    products: List<ProductDetails>,
    onDismiss: () -> Unit,
    onDonateClick: (ProductDetails) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Support Developer",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Your contribution helps keep the app ad-free.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Горизонтальный скролл для кнопок доната
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    items(products) { product ->
                        Button(onClick = { onDonateClick(product) }) {
                            Text("${product.name} (${product.oneTimePurchaseOfferDetails?.formattedPrice})")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, navController: NavController) {
    val theme by viewModel.theme.collectAsState(initial = "light")
    val notificationSettings by viewModel.notificationSettings.collectAsState(
        initial = com.gemini.bibliverse.data.DataStoreManager.NotificationSettings(false, 8, 0)
    )
    val affirmationsEnabled by viewModel.affirmationsEnabled.collectAsState(initial = true)

    // --- Состояния для донатов ---
    val donationProducts by viewModel.donationProducts.collectAsState()
    val billingMessage by viewModel.billingMessage.collectAsState()

    var showDonationDialog by remember { mutableStateOf(false) }

    var notificationsEnabled by remember(notificationSettings.enabled) { mutableStateOf(notificationSettings.enabled) }
    var notificationHour by remember(notificationSettings.hour) { mutableIntStateOf(notificationSettings.hour) }
    var notificationMinute by remember(notificationSettings.minute) { mutableIntStateOf(notificationSettings.minute) }

    val context = LocalContext.current
    val activity = (context as? Activity)

    if (showDonationDialog) {
        DonationDialog(
            products = donationProducts,
            onDismiss = { showDonationDialog = false },
            onDonateClick = { product ->
                if (activity != null) {
                    viewModel.initiateDonation(activity, product)
                }
                showDonationDialog = false
            }
        )
    }

    LaunchedEffect(billingMessage) {
        billingMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearBillingMessage()
        }
    }
    // FIX 1.4: Запрос разрешения на уведомления для Android 13+
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                notificationsEnabled = true
                viewModel.setNotifications(true, notificationHour, notificationMinute)
            }
        }
    )

    val is24HourFormat = DateFormat.is24HourFormat(context)
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour: Int, minute: Int ->
            notificationHour = hour
            notificationMinute = minute
            // Устанавливаем уведомление сразу после выбора времени
            viewModel.setNotifications(true, hour, minute)
        }, notificationHour, notificationMinute, is24HourFormat
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Daily Verse Notification", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { isChecked ->
                        notificationsEnabled = isChecked
                        if (isChecked) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                if (!alarmManager.canScheduleExactAlarms()) {
                                    Intent().also { intent ->
                                        intent.action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                                        context.startActivity(intent)
                                    }
                                }
                            }
                            // Устанавливаем уведомление с текущим временем
                            viewModel.setNotifications(true, notificationHour, notificationMinute)
                        } else {
                            // Отменяем уведомление сразу
                            viewModel.setNotifications(false, notificationHour, notificationMinute)
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // --- Affirmations Section ---
            Text("User Affirmations", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable my affirmations")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = affirmationsEnabled,
                    onCheckedChange = { viewModel.setAffirmationsEnabled(it) }
                )
            }
            if (affirmationsEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { navController.navigate(Screen.Affirmations.route) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit My Affirmations")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            // --- Секция для донатов ---
            Text("Support App Developer", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { showDonationDialog = true },
                modifier = Modifier.fillMaxWidth(),
                // Кнопка неактивна, пока товары не загружены
                enabled = donationProducts.isNotEmpty()
            ) {
                Text("Make a Donation")
            }
        }
    }
}
