package com.crsk.openclaw.ui.settings

/**
 * Pure Kotlin parser for the native OpenClaw terminal's device-code screen.
 *
 * Important: this data is sensitive. Never log history or the returned code.
 * The running buffer is bounded and exists only for the active login attempt.
 */
internal class CodexDeviceCodeCapture {
    data class Authorization(val code: String?, val url: String?)

    private val recent = StringBuilder()
    private val ansiEscape = Regex("""\x1B(?:\[[0-?]*[ -/]*[@-~]|\][^\x07]*(?:\x07|\x1B\\))""")
    private val codePattern = Regex(
        """(?i)\b(?:user\s+|device\s+)?code\s*:\s*([A-Z0-9]{4,6})\s*-\s*([A-Z0-9]{3,6})(?![A-Z0-9])"""
    )
    private val linkPattern = Regex("""https://[^\s<>"']+""")

    fun consume(fragment: String): Authorization {
        recent.append(fragment)
        if (recent.length > 8192) recent.delete(0, recent.length - 8192)

        val normalized = recent.toString()
            .replace(ansiEscape, "")
            .replace('\r', '\n')

        // A device code may have been split between terminal write() chunks,
        // and its letters may be interspersed with terminal color escapes.
        val match = codePattern.find(normalized)
        val code = match?.let {
            it.groupValues[1].uppercase() + "-" + it.groupValues[2].uppercase()
        }

        val url = linkPattern.findAll(normalized)
            .map { it.value.trimEnd('│', '.', ',', ')', ']', '\u001B') }
            .firstOrNull {
                runCatching { java.net.URI(it).host == "auth.openai.com" }.getOrDefault(false)
            }

        return Authorization(code, url)
    }
}
