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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import lukulent.mindtoss.app.data.model.SendStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditEntry: (() -> Unit)? = null,
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
                    placeholder = { Text("onboarding@resend.dev") },
                    supportingText = { Text("Leer lassen für Resend-Default") },
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
                    onEdit = {
                        viewModel.editHistoryEntry(entry)
                        onEditEntry?.invoke()
                    },
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
    onEdit: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY) }
    val typeIcon = if (entry.type == MessageType.NOTE) "\uD83D\uDCE7" else "\u2705"
    val statusIcon = when (entry.status) {
        SendStatus.SUCCESS -> "\u2714\uFE0F"
        SendStatus.FAILED -> "\u274C"
        SendStatus.QUEUED -> "\u23F3"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = when (entry.status) {
            SendStatus.FAILED -> CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            )
            else -> CardDefaults.cardColors()
        },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "$typeIcon $statusIcon  ${dateFormat.format(Date(entry.timestamp))}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (entry.errorMessage != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    entry.errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                entry.content.take(200),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Bearbeiten", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onResend) {
                    Icon(Icons.Default.Refresh, contentDescription = "Senden", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Kopieren", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Löschen", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
