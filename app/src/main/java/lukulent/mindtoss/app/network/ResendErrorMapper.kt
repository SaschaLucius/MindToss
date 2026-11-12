package lukulent.mindtoss.app.network

data class UserFacingError(
    val message: String,
    val details: String,
)

object ResendErrorMapper {

    fun map(raw: String?): UserFacingError {
        val details = raw?.takeIf { it.isNotBlank() } ?: "Unbekannter Fehler"
        val message = when {
            details.contains("401") || details.contains("Unauthorized", ignoreCase = true) ->
                "API-Key ungültig. Bitte in den Einstellungen prüfen."
            details.contains("403") || details.contains("Forbidden", ignoreCase = true) ->
                "Keine Berechtigung. Domain oder API-Key prüfen."
            details.contains("422") || details.contains("validation", ignoreCase = true) ->
                "Ungültige Anfrage. Absender- und Empfänger-Adresse prüfen."
            details.contains("429") || details.contains("rate", ignoreCase = true) ->
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
