package lukulent.mindtoss.app.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MailFormatTest {

    @Test
    fun subjectAndBody_splitsOnFirstLine() {
        val parts = MailFormat.subjectAndBody("Buy milk\n2 liters")
        assertEquals("Buy milk", parts.subject)
        assertEquals("2 liters", parts.body)
    }

    @Test
    fun subjectAndBody_singleLineUsesSubjectAsBody() {
        val parts = MailFormat.subjectAndBody("Only subject")
        assertEquals("Only subject", parts.subject)
        assertEquals("Only subject", parts.body)
    }

    @Test
    fun subjectAndBody_blankSecondLineFallsBackToSubject() {
        val parts = MailFormat.subjectAndBody("Title\n\n")
        assertEquals("Title", parts.subject)
        assertEquals("Title", parts.body)
    }

    @Test
    fun taskChunks_smartSplitOnBlankLines() {
        val chunks = MailFormat.taskChunks("One\n\nTwo\n\n\nThree", smartSplit = true)
        assertEquals(listOf("One", "Two", "Three"), chunks)
    }

    @Test
    fun taskChunks_keepsWholeTextWhenDisabled() {
        val text = "One\n\nTwo"
        assertEquals(listOf(text), MailFormat.taskChunks(text, smartSplit = false))
    }

    @Test
    fun permanentFailure_on422() {
        assertTrue(ResendErrorMapper.isPermanentFailure("HTTP 422: validation error"))
    }

    @Test
    fun permanentFailure_notOn429() {
        assertFalse(ResendErrorMapper.isPermanentFailure("HTTP 429: rate limited"))
    }

    @Test
    fun permanentFailure_notOnNetwork() {
        assertFalse(ResendErrorMapper.isPermanentFailure("Unable to resolve host"))
    }

    @Test
    fun map_includesHttpCodeInMessage() {
        val mapped = ResendErrorMapper.map("HTTP 422: {\"message\":\"validation_error\"}")
        assertTrue(mapped.message.contains("HTTP 422"))
    }

    @Test
    fun map_omitsHttpCodeForNetworkErrors() {
        val mapped = ResendErrorMapper.map("Unable to resolve host api.resend.com")
        assertFalse(mapped.message.contains("HTTP"))
    }
}
