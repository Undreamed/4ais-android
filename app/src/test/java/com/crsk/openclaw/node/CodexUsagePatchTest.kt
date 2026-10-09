package com.crsk.openclaw.node

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class CodexUsagePatchTest {
    // Original fetcher from OpenClaw 2026.5.12 (MIT), retained as an integration fixture.
    private val original = javaClass.getResource("/codex-usage-2026.5.12.js")!!.readText().replace("\r\n", "\n")
    private val snippet = File("src/main/assets/codex-plan-usage.js").readText()

    @Test fun patchesThePinnedFetcherWithoutChangingAuthOrHttpErrors() {
        val patched = CodexUsagePatch.apply(original, snippet)!!
        val parseStart = original.indexOf("\tconst windows = [];")
        assertEquals(original.substring(0, parseStart), patched.substring(0, parseStart))
        assertFalse(patched.contains("balance.toFixed"))
        assertTrue(patched.contains("FOURAIS_CODEX_PLAN_USAGE_V1"))
        assertEquals(patched, CodexUsagePatch.apply(patched, snippet))
    }

    @Test fun incompatibleSourceIsLeftUntouched() {
        assertNull(CodexUsagePatch.apply(original.replace("data.credits?.balance", "changedBalance"), snippet))
        assertNull(CodexUsagePatch.apply("const windows = [];", snippet))
    }
}
