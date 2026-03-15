package lukulent.mindtoss.app.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class SendEmailRequest(
    val from: String,
    val to: List<String>,
    val subject: String,
    val text: String,
)

object ResendApi {

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun sendEmail(
        apiKey: String,
        from: String,
        to: String,
        subject: String,
        body: String,
    ): Result<Unit> {
        return try {
            val response = client.post("https://api.resend.com/emails") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(
                    SendEmailRequest(
                        from = from,
                        to = listOf(to),
                        subject = subject,
                        text = body,
                    )
                )
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(Exception("HTTP ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
