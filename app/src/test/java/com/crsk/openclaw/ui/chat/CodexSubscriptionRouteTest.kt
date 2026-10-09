package com.crsk.openclaw.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class CodexSubscriptionRouteTest {
    @Test fun previousCodexSelectionFallsBackToDeviceVerifiedModel() {
        assertEquals("openai-codex" to "gpt-5.5", selectProvider("openai-codex", "gpt-5.4"))
    }

    @Test fun currentCodexModelKeepsItsOAuthProvider() {
        assertEquals("openai-codex" to "gpt-5.5", selectProvider("openai-codex", "gpt-5.5"))
    }

    @Test fun allGptSixSelectionsKeepTheirOAuthRoute() {
        for (id in listOf("gpt-6.1-sol", "gpt-6-astra", "gpt-6-sol", "gpt-6-luna")) {
            assertEquals("openai-codex" to id, selectProvider("openai-codex", id))
        }
    }

    @Test fun normalKeyProviderKeepsProviderId() {
        val (provider, _) = selectProvider("gem", "gemini-2.5-flash")
        assertEquals("gem", provider)
    }
}
