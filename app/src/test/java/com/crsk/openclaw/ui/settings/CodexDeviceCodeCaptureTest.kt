package com.crsk.openclaw.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CodexDeviceCodeCaptureTest {
    @Test fun readsPlainRichTerminalOutput() {
        val parser = CodexDeviceCodeCapture()
        val actual = parser.consume(
            "◇  OpenAI Codex device code\r\n│  URL: https://auth.openai.com/codex/device  │\r\n" +
            "│  Code: ABCD-EFGHI  │\r\nCode expires in 15 minutes."
        )
        assertEquals("ABCD-EFGHI", actual.code)
        assertEquals("https://auth.openai.com/codex/device", actual.url)
    }

    @Test fun readsCodeWithoutTerminatingNewline() {
        val parser = CodexDeviceCodeCapture()
        parser.consume("│  URL: https://auth.openai.com/codex/device\r\n")
        val actual = parser.consume("│ Code: WXYZ-12345")
        assertEquals("WXYZ-12345", actual.code)
    }

    @Test fun acceptsChunksAndTerminalColorEscapeSequences() {
        val parser = CodexDeviceCodeCapture()
        parser.consume("\u001B[?25lURL: https://auth.openai.com/codex/device\nCode: \u001B[32mAB")
        val actual = parser.consume("CD\u001B[0m-\u001B[1m12345\u001B[0m")
        assertEquals("ABCD-12345", actual.code)
    }

    @Test fun doesNotMisreadCodeExpiryOrRandomLinks() {
        val parser = CodexDeviceCodeCapture()
        val result = parser.consume("Code expires in 15 minutes\nhttps://example.com/test")
        assertNull(result.code)
        assertNull(result.url)
    }

    @Test fun rejectsCodeFromUntrustedHost() {
        val parser = CodexDeviceCodeCapture()
        val actual = parser.consume("URL: https://auth.openai.com.evil.example/device")
        assertNull(actual.url)
    }
}
