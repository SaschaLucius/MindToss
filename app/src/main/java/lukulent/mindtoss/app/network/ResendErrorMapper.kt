package lukulent.mindtoss.app.network

data class UserFacingError(
    val message: String,
    val details: String,
)

object ResendErrorMapper {

    private val httpStatus = Regex("""HTTP (\d{3})""")

    /** Client errors that will not succeed on retry. 429 is excluded so the queue backs off. */
    fun isPermanentFailure(raw: String?): Boolean {
        val details = raw ?: return false
        val code = httpCode(details) ?: return false
        return code in 400..499 && code != 429
    }

    fun map(raw: String?): UserFacingError {
        val details = raw?.takeIf { it.isNotBlank() } ?: "Unbekannter Fehler"
        val code = httpCode(details)
        val lower = details.lowercase()
        val base = when {
            // Auth / API key
            lower.contains("missing_api_key") || lower.contains("missing api key") ->
                "API-Key fehlt. Bitte in den Einstellungen eintragen."
            lower.contains("suspended_api_key") || lower.contains("api key is suspended") ->
                "API-Key gesperrt. In Resend prüfen oder neuen Key erstellen."
            lower.contains("api key is not active") ||
                (lower.contains("restricted_api_key") && code == 403) ->
                "API-Key inaktiv. In Resend prüfen oder neuen Key erstellen."
            lower.contains("invalid_api_key") ||
                lower.contains("api key is invalid") ||
                code == 401 ||
                lower.contains("unauthorized") ->
                "API-Key ungültig. Bitte in den Einstellungen prüfen."

            // Domain / from / testing restrictions (common with onboarding@resend.dev)
            lower.contains("only send testing emails") ||
                lower.contains("only send test emails") ->
                "Test-Absender erlaubt nur die eigene Resend-Konto-E-Mail. Domain verifizieren oder Empfänger anpassen."
            lower.contains("domain is not verified") ||
                lower.contains("invalid_from_address") ->
                "Absender-Domain nicht verifiziert. In Resend prüfen und Absender anpassen."

            // Content / validation
            lower.contains("missing `html` or `text`") ||
                lower.contains("missing html or text") ->
                "E-Mail-Text fehlt. Bitte erneut versuchen."
            lower.contains("missing_required_field") ||
                lower.contains("missing_required_parameter") ->
                "Pflichtfeld fehlt in der Anfrage. Absender, Empfänger und Text prüfen."
            code == 400 ||
                code == 422 ||
                lower.contains("validation_error") ||
                lower.contains("validation") ->
                "Ungültige Anfrage. Absender- und Empfänger-Adresse prüfen."

            // Quotas / rate limits
            lower.contains("daily_quota_exceeded") ||
                lower.contains("daily email sending quota") ->
                "Tägliches Versandlimit erreicht. Morgen erneut versuchen oder Plan upgraden."
            lower.contains("monthly_quota_exceeded") ||
                lower.contains("monthly email sending quota") ->
                "Monatliches Versandlimit erreicht. Plan upgraden oder später erneut versuchen."
            code == 429 || lower.contains("rate_limit_exceeded") ->
                "Zu viele Anfragen. Bitte kurz warten und erneut versuchen."

            // Permissions leftover
            code == 403 || lower.contains("forbidden") || lower.contains("invalid_permission") ->
                "Keine Berechtigung. Domain oder API-Key prüfen."

            // Resend server / availability
            code == 500 ||
                lower.contains("application_error") ||
                lower.contains("internal_server_error") ->
                "Resend-Serverfehler. Später erneut versuchen."
            code == 503 ||
                lower.contains("service_unavailable") ||
                lower.contains("api is temporarily unavailable") ->
                "Resend vorübergehend nicht erreichbar. Später erneut versuchen."

            // Local network
            lower.contains("unable to resolve host") ||
                lower.contains("unknownhost") ||
                lower.contains("timeout") ->
                "Netzwerkfehler. Verbindung prüfen und erneut versuchen."

            else ->
                "Senden fehlgeschlagen. Tippe auf „Details kopieren“ für technische Infos."
        }
        val message = if (code != null) "$base (HTTP $code)" else base
        return UserFacingError(message = message, details = details)
    }

    private fun httpCode(details: String): Int? =
        httpStatus.find(details)?.groupValues?.get(1)?.toIntOrNull()
}
