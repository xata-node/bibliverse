// --- НОВЫЙ ФАЙЛ: ui/screens/AboutScreen.kt ---
package com.gemini.bibliverse.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gemini.bibliverse.R
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
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
                .verticalScroll(rememberScrollState()) // Make the column scrollable
        ) {
            // App Description Section
            Text(
                text = stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Version Info
            InfoRow(title = stringResource(R.string.version_title), value = stringResource(R.string.version_number))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Developer Info
            InfoRow(title = stringResource(R.string.developer_title), value = stringResource(R.string.developer_name))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Links Section
            Text(
                text = stringResource(R.string.links_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            // Privacy Policy Link
            ClickableLinkRow(
                text = stringResource(R.string.privacy_policy),
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, context.getString(R.string.privacy_policy_url).toUri())
                    context.startActivity(intent)
                }
            )
            // Feedback Email Link
            ClickableLinkRow(
                text = stringResource(R.string.feedback),
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = "mailto:".toUri()
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(context.getString(R.string.feedback_email)))
                        putExtra(Intent.EXTRA_SUBJECT, "Feedback for Bibliverse App")
                    }
                    context.startActivity(Intent.createChooser(intent, "Send Feedback"))
                }
            )

            // Credits Section
            Text(
                text = stringResource(R.string.credits_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            // Icons Credits Link
            ClickableLinkRow(
                text = stringResource(R.string.icons_source_prefix),
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW,context.getString(R.string.icons_url).toUri())
                    context.startActivity(intent)
                }
            )
        }
    }
}

// Reusable component for simple info rows
@Composable
private fun InfoRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

// Reusable component for clickable link rows
@Composable
private fun ClickableLinkRow(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.weight(1f))
    }
}
