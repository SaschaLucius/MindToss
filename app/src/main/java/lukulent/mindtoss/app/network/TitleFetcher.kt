package lukulent.mindtoss.app.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

object TitleFetcher {

    private val client = HttpClient(OkHttp)
    private val titleRegex = Regex("<title[^>]*>([^<]+)</title>", RegexOption.IGNORE_CASE)

    suspend fun fetchTitle(url: String): String? {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return null
        return try {
            val response = client.get(url)
            val html = response.bodyAsText()
            titleRegex.find(html)?.groupValues?.get(1)?.trim()
        } catch (_: Exception) {
            null
        }
    }
}
