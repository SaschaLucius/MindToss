package lukulent.mindtoss.app.ui.main

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.speech.RecognizerIntent
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import lukulent.mindtoss.app.data.model.MessageType
import java.util.Locale
import android.content.ClipData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    onCloseApp: () -> Unit,
    viewModel: MainViewModel = viewModel(),
    previewSuccessOverlay: Boolean = false,
) {
    val draftText by viewModel.draftText.collectAsStateWithLifecycle()
    var textFieldValue by remember { mutableStateOf(TextFieldValue()) }

    // Sync ViewModel → TextFieldValue (external changes: share, edit, clear)
    LaunchedEffect(draftText) {
        if (draftText != textFieldValue.text) {
            textFieldValue = TextFieldValue(
                text = draftText,
                selection = TextRange(draftText.length),
            )
        }
    }

    val taskRecipient by viewModel.taskRecipient.collectAsStateWithLifecycle()
    val isConfigured by viewModel.isConfigured.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val hasPendingWork by viewModel.hasPendingWork.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboard.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val view = LocalView.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var showSuccess by remember { mutableStateOf(previewSuccessOverlay) }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.appendToDraft(spoken)
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.sendSuccess.collect {
            keyboardController?.hide()
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            showSuccess = true
            delay(450)
            onCloseApp()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.queued.collect {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            snackbarHostState.showSnackbar(
                message = "Offline – in Warteschlange, wird automatisch gesendet",
                duration = SnackbarDuration.Short,
            )
            delay(900)
            onCloseApp()
        }
    }

    LaunchedEffect(Unit) {
        if (previewSuccessOverlay) {
            keyboardController?.hide()
        } else {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(error) {
        error?.let { err ->
            keyboardController?.hide()
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            val result = snackbarHostState.showSnackbar(
                message = err.message,
                actionLabel = "Details kopieren",
                withDismissAction = true,
                duration = SnackbarDuration.Indefinite,
            )
            if (result == SnackbarResult.ActionPerformed) {
                clipboard.setClipEntry(ClipData.newPlainText("MindToss Fehler", err.details).toClipEntry())
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (!isLandscape) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("MindToss")
                                if (hasPendingWork) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    BadgedBox(
                                        badge = { Badge() },
                                        modifier = Modifier.semantics {
                                            contentDescription = "Ausstehende Nachrichten in der Warteschlange"
                                        },
                                    ) {
                                        Text(
                                            "Warteschlange",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                            }
                        },
                    )
                }
            },
        ) { padding ->
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .imePadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (!isConfigured) {
                            SetupBanner(onClick = onNavigateToSettings)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        DraftField(
                            textFieldValue = textFieldValue,
                            onValueChange = {
                                textFieldValue = it
                                viewModel.updateDraft(it.text)
                            },
                            focusRequester = focusRequester,
                            onVoiceClick = {
                                launchVoice(voiceLauncher)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                        }
                        if (hasPendingWork) {
                            Text(
                                "Warteschlange aktiv",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.semantics {
                                    contentDescription = "Ausstehende Nachrichten in der Warteschlange"
                                },
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        SendButtons(
                            isSending = isSending,
                            canSend = draftText.isNotBlank() && !showSuccess,
                            showTask = taskRecipient.isNotBlank(),
                            vertical = true,
                            onSendNote = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.send(MessageType.NOTE)
                            },
                            onSendTask = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.send(MessageType.TASK)
                            },
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .imePadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                ) {
                    if (!isConfigured) {
                        SetupBanner(onClick = onNavigateToSettings)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    DraftField(
                        textFieldValue = textFieldValue,
                        onValueChange = {
                            textFieldValue = it
                            viewModel.updateDraft(it.text)
                        },
                        focusRequester = focusRequester,
                        onVoiceClick = {
                            launchVoice(voiceLauncher)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SendButtons(
                        isSending = isSending,
                        canSend = draftText.isNotBlank() && !showSuccess,
                        showTask = taskRecipient.isNotBlank(),
                        vertical = false,
                        onSendNote = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.send(MessageType.NOTE)
                        },
                        onSendTask = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.send(MessageType.TASK)
                        },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showSuccess,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Erfolgreich gesendet",
                        modifier = Modifier.size(88.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Gesendet",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Resend einrichten",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    "API-Key und Empfänger fehlen noch",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                "Öffnen",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DraftField(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = textFieldValue,
        onValueChange = onValueChange,
        modifier = modifier.focusRequester(focusRequester),
        shape = RoundedCornerShape(16.dp),
        placeholder = { Text("Was denkst du?") },
        trailingIcon = {
            IconButton(onClick = onVoiceClick) {
                Icon(Icons.Default.Mic, contentDescription = "Spracheingabe")
            }
        },
    )
}

@Composable
private fun SendButtons(
    isSending: Boolean,
    canSend: Boolean,
    showTask: Boolean,
    vertical: Boolean,
    onSendNote: () -> Unit,
    onSendTask: () -> Unit,
) {
    val noteButton: @Composable (Modifier) -> Unit = { buttonModifier ->
        Button(
            onClick = onSendNote,
            enabled = !isSending && canSend,
            modifier = buttonModifier.heightIn(min = 48.dp),
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(
                    Icons.Default.Email,
                    contentDescription = "Als Notiz senden",
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Notiz")
        }
    }

    val taskButton: @Composable (Modifier) -> Unit = { buttonModifier ->
        Button(
            onClick = onSendTask,
            enabled = !isSending && canSend,
            modifier = buttonModifier.heightIn(min = 48.dp),
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Als Aufgabe senden",
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Aufgabe")
        }
    }

    if (vertical) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            noteButton(Modifier)
            if (showTask) taskButton(Modifier)
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            noteButton(Modifier.weight(1f))
            if (showTask) taskButton(Modifier.weight(1f))
        }
    }
}

private fun launchVoice(
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
    }
    try {
        launcher.launch(intent)
    } catch (_: Exception) {
        // Speech recognizer unavailable on this device
    }
}
