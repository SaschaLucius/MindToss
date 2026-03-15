package lukulent.mindtoss.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lukulent.mindtoss.app.data.model.HistoryEntry
import lukulent.mindtoss.app.data.model.MessageType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val senderEmail by viewModel.senderEmail.collectAsStateWithLifecycle()
    val noteRecipient by viewModel.noteRecipient.collectAsStateWithLifecycle()
    val taskRecipient by viewModel.taskRecipient.collectAsStateWithLifecycle()
    val fetchTitle by viewModel.fetchTitle.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showApiKey by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.message.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // --- Resend Configuration ---
            item { SectionHeader("Resend Konfiguration") }

            item {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { viewModel.updateApiKey(it) },
                    label = { Text("Resend API-Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showApiKey) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = null,
                            )
                        }
                    },
                )
            }

            item {
                OutlinedTextField(
                    value = senderEmail,
                    onValueChange = { viewModel.updateSenderEmail(it) },
                    label = { Text("Absender-E-Mail") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            // --- Recipients ---
            item { SectionHeader("Empfänger") }

            item {
                OutlinedTextField(
                    value = noteRecipient,
                    onValueChange = { viewModel.updateNoteRecipient(it) },
                    label = { Text("Empfänger – Notizen (Pflicht)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            item {
                OutlinedTextField(
                    value = taskRecipient,
                    onValueChange = { viewModel.updateTaskRecipient(it) },
                    label = { Text("Empfänger – Tasks (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            // --- Behavior ---
            item { SectionHeader("Verhalten") }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Seiten-Titel bei Links abrufen",
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = fetchTitle,
                        onCheckedChange = { viewModel.updateFetchTitle(it) },
                    )
                }
            }

            // --- Appearance ---
            item { SectionHeader("Erscheinungsbild") }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "system" to "System",
                        "light" to "Hell",
                        "dark" to "Dunkel",
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = theme == value,
                            onClick = { viewModel.updateTheme(value) },
                            label = { Text(label) },
                        )
                    }
                }
            }

            // --- History ---
            item { SectionHeader("Historie") }

            if (history.isEmpty()) {
                item {
                    Text(
                        "Keine gesendeten Nachrichten",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(
                items = history.sortedByDescending { it.timestamp },
                key = { it.id },
            ) { entry ->
                HistoryItem(
                    entry = entry,
                    onResend = { viewModel.resendHistoryEntry(entry) },
                    onDelete = { viewModel.deleteHistoryEntry(entry.id) },
                    onCopy = { viewModel.copyHistoryEntry(entry) },
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun HistoryItem(
    entry: HistoryEntry,
    onResend: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY) }
    val typeIcon = if (entry.type == MessageType.NOTE) "\uD83D\uDCE7" else "\u2705"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "$typeIcon  ${dateFormat.format(Date(entry.timestamp))}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                entry.content.take(200),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onResend, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Senden")
                }
                TextButton(onClick = onCopy, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kopieren")
                }
                TextButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Löschen")
                }
            }
        }
    }
}
