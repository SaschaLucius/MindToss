package lukulent.mindtoss.app.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResendErrorMapperTest {

    @Test
    fun mapsMissingApiKey() {
        val msg = ResendErrorMapper.map("""HTTP 401: {"name":"missing_api_key","message":"Missing API Key"}""").message
        assertTrue(msg.contains("API-Key fehlt"))
        assertTrue(msg.contains("HTTP 401"))
    }

    @Test
    fun mapsInvalidApiKey() {
        val msg = ResendErrorMapper.map("""HTTP 403: {"name":"invalid_api_key","message":"API key is invalid"}""").message
        assertTrue(msg.contains("API-Key ungültig"))
    }

    @Test
    fun mapsSuspendedApiKey() {
        val msg = ResendErrorMapper.map("""HTTP 403: {"name":"suspended_api_key","message":"This API key is suspended"}""").message
        assertTrue(msg.contains("gesperrt"))
    }

    @Test
    fun mapsTestingRecipientRestriction() {
        val msg = ResendErrorMapper.map(
            """HTTP 403: {"name":"validation_error","message":"You can only send testing emails to your own email address (you@example.com)."}""",
        ).message
        assertTrue(msg.contains("Test-Absender"))
    }

    @Test
    fun mapsUnverifiedDomain() {
        val msg = ResendErrorMapper.map(
            """HTTP 403: {"name":"validation_error","message":"The example.com domain is not verified. Please, add and verify your domain."}""",
        ).message
        assertTrue(msg.contains("nicht verifiziert"))
    }

    @Test
    fun mapsDailyQuota() {
        val msg = ResendErrorMapper.map(
            """HTTP 429: {"name":"daily_quota_exceeded","message":"You have exceeded your daily email sending quota."}""",
        ).message
        assertTrue(msg.contains("Tägliches Versandlimit"))
        assertTrue(msg.contains("HTTP 429"))
    }

    @Test
    fun mapsMonthlyQuota() {
        val msg = ResendErrorMapper.map(
            """HTTP 429: {"name":"monthly_quota_exceeded","message":"You have exceeded your monthly email sending quota."}""",
        ).message
        assertTrue(msg.contains("Monatliches Versandlimit"))
    }

    @Test
    fun mapsRateLimit() {
        val msg = ResendErrorMapper.map(
            """HTTP 429: {"name":"rate_limit_exceeded","message":"Too many requests."}""",
        ).message
        assertTrue(msg.contains("Zu viele Anfragen"))
    }

    @Test
    fun mapsServerError() {
        val msg = ResendErrorMapper.map(
            """HTTP 500: {"name":"application_error","message":"An unexpected error occurred."}""",
        ).message
        assertTrue(msg.contains("Resend-Serverfehler"))
    }

    @Test
    fun mapsServiceUnavailable() {
        val msg = ResendErrorMapper.map(
            """HTTP 503: {"name":"service_unavailable","message":"API is temporarily unavailable"}""",
        ).message
        assertTrue(msg.contains("nicht erreichbar"))
    }

    @Test
    fun mapsMissingTextField() {
        val msg = ResendErrorMapper.map(
            """HTTP 422: {"name":"missing_required_field","message":"Missing `html` or `text` field."}""",
        ).message
        assertTrue(msg.contains("E-Mail-Text fehlt"))
    }

    @Test
    fun permanentFailureExcludes429AndNetwork() {
        assertTrue(ResendErrorMapper.isPermanentFailure("HTTP 422: validation error"))
        assertFalse(ResendErrorMapper.isPermanentFailure("HTTP 429: rate limited"))
        assertFalse(ResendErrorMapper.isPermanentFailure("Unable to resolve host"))
        assertFalse(ResendErrorMapper.isPermanentFailure("HTTP 500: boom"))
    }

    @Test
    fun mapOmitsHttpCodeForNetworkErrors() {
        val mapped = ResendErrorMapper.map("Unable to resolve host api.resend.com")
        assertEquals(
            "Netzwerkfehler. Verbindung prüfen und erneut versuchen.",
            mapped.message,
        )
    }
}
