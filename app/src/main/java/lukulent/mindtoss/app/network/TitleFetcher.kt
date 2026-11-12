package lukulent.mindtoss.app.network

import android.text.Html
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readBuffer
import kotlinx.io.readString

object TitleFetcher {

    private const val MAX_BYTES = 64 * 1024L
    private val client = HttpClient(OkHttp)
    private val titleRegex = Regex("<title[^>]*>([^<]+)</title>", RegexOption.IGNORE_CASE)

    fun sanitizeUrl(url: String): String {
        return url.trimEnd('.', ',', ';', ':', ')', ']', '}', '"', '\'')
    }

    suspend fun fetchTitle(url: String): String? {
        val cleaned = sanitizeUrl(url)
        if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) return null
        return try {
            val response = client.get(cleaned)
            val html = response.bodyAsChannel().readBuffer(MAX_BYTES).readString()
            titleRegex.find(html)?.groupValues?.get(1)?.trim()?.let { raw ->
                Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString()
            }
        } catch (_: Exception) {
            null
        }
    }
}
