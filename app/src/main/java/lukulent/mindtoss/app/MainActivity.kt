package lukulent.mindtoss.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lukulent.mindtoss.app.data.SettingsRepository
import lukulent.mindtoss.app.network.TitleFetcher
import lukulent.mindtoss.app.ui.main.MainScreen
import lukulent.mindtoss.app.ui.main.MainViewModel
import lukulent.mindtoss.app.ui.settings.SettingsScreen
import lukulent.mindtoss.app.ui.theme.MindTossTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleShareIntent(intent)

        setContent {
            val settingsRepo = remember { SettingsRepository(applicationContext) }
            val theme by settingsRepo.theme.collectAsStateWithLifecycle(initialValue = "system")
            val navController = rememberNavController()

            MindTossTheme(themeMode = theme) {
                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        val viewModel: MainViewModel = viewModel()
                        MainScreen(
                            onNavigateToSettings = { navController.navigate("settings") },
                            onCloseApp = { finishAffinity() },
                            viewModel = viewModel,
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") return
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
        val sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT)

        val settingsRepo = SettingsRepository(applicationContext)
        lifecycleScope.launch {
            val shouldFetchTitle = settingsRepo.fetchTitle.first()
            val processedText = if (shouldFetchTitle) {
                processSharedText(sharedText, sharedSubject)
            } else {
                sharedText
            }
            settingsRepo.setDraft(processedText)
        }
    }

    private suspend fun processSharedText(text: String, subject: String?): String {
        val urlRegex = Regex("https?://\\S+")
        val lines = text.lines()

        // Single line that is just a URL
        if (lines.size == 1 && urlRegex.matches(text.trim())) {
            val url = text.trim()
            // Prefer EXTRA_SUBJECT if available, otherwise fetch title
            val title = subject?.takeIf { it.isNotBlank() } ?: TitleFetcher.fetchTitle(url)
            return if (title != null) "$title\n$url" else url
        }

        // Multi-line: first line = subject (don't fetch), rest = body (fetch titles)
        if (lines.size > 1) {
            val subject = lines.first()
            val body = lines.drop(1).joinToString("\n")
            val processedBody = insertTitlesForUrls(body, urlRegex)
            return "$subject\n$processedBody"
        }

        // Single line with URL inside other text
        return insertTitlesForUrls(text, urlRegex)
    }

    private suspend fun insertTitlesForUrls(text: String, urlRegex: Regex): String {
        val matches = urlRegex.findAll(text).toList()
        if (matches.isEmpty()) return text

        var result = text
        for (match in matches.reversed()) {
            val url = match.value
            val title = TitleFetcher.fetchTitle(url)
            if (title != null) {
                result = result.substring(0, match.range.first) +
                    "$title\n${match.value}" +
                    result.substring(match.range.last + 1)
            }
        }
        return result
    }
}
