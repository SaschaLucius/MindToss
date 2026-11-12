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
        val code = httpStatus.find(details)?.groupValues?.get(1)?.toIntOrNull() ?: return false
        return code in 400..499 && code != 429
    }

    fun map(raw: String?): UserFacingError {
        val details = raw?.takeIf { it.isNotBlank() } ?: "Unbekannter Fehler"
        val message = when {
            details.contains("401") || details.contains("Unauthorized", ignoreCase = true) ->
                "API-Key ungültig. Bitte in den Einstellungen prüfen."
            details.contains("403") || details.contains("Forbidden", ignoreCase = true) ->
                "Keine Berechtigung. Domain oder API-Key prüfen."
            details.contains("Missing `html` or `text`", ignoreCase = true) ||
                details.contains("Missing html or text", ignoreCase = true) ->
                "E-Mail-Text fehlt. Bitte erneut versuchen."
            details.contains("422") || details.contains("validation", ignoreCase = true) ->
                "Ungültige Anfrage. Absender- und Empfänger-Adresse prüfen."
            details.contains("429") ->
                "Zu viele Anfragen. Bitte kurz warten und erneut versuchen."
            details.contains("Unable to resolve host", ignoreCase = true) ||
                details.contains("UnknownHost", ignoreCase = true) ||
                details.contains("timeout", ignoreCase = true) ->
                "Netzwerkfehler. Verbindung prüfen und erneut versuchen."
            else ->
                "Senden fehlgeschlagen. Tippe auf „Details kopieren“ für technische Infos."
        }
        return UserFacingError(message = message, details = details)
    }
}
