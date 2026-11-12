package lukulent.mindtoss.app.network

object MailFormat {

    data class Parts(val subject: String, val body: String)

    fun subjectAndBody(text: String): Parts {
        val lines = text.lines()
        val subject = lines.first()
        val body = lines.drop(1).joinToString("\n")
        return Parts(subject = subject, body = body)
    }

    fun taskChunks(text: String, smartSplit: Boolean): List<String> {
        return if (smartSplit) {
            text.split(Regex("\n\\s*\n")).map { it.trim() }.filter { it.isNotBlank() }
        } else {
            listOf(text)
        }
    }
}
