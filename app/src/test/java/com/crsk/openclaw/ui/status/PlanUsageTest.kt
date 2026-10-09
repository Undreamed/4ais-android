package com.crsk.openclaw.ui.status

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PlanUsageTest {
    @Test fun readsOnlyCodexAccountAndConvertsUsedToRemaining() {
        val result = PlanUsage.parse(JSONObject("""{"updatedAt":123,"providers":[
          {"provider":"anthropic","windows":[{"label":"Week","usedPercent":99}]},
          {"provider":"openai-codex","plan":"pro","windows":[{"label":"5h","usedPercent":27,"resetAt":1900000000000}],
           "credits":{"hasCredits":true,"unlimited":false,"balance":42.5}}
        ]}"""))!!
        assertEquals("pro", result.plan)
        assertEquals(73.0, result.windows.single().remainingPercent, 0.001)
        assertEquals(1900000000000L, result.windows.single().resetAt)
        assertEquals(42.5, result.credits!!.balance!!, 0.001)
    }

    @Test fun missingPercentageIsNotShownAsFullyRemaining() {
        val result = PlanUsage.parse(JSONObject("""{"providers":[{"provider":"openai-codex",
          "windows":[{"label":"5h"},{"label":"Week","usedPercent":null}],"credits":{"balance":null}}]}"""))!!
        assertTrue(result.windows.isEmpty())
        assertNull(result.credits!!.balance)
    }

    @Test fun absentCodexAndProviderErrorsDoNotBecomeSuccessfulUsage() {
        assertNull(PlanUsage.parse(JSONObject("""{"providers":[]}""")))
        assertNull(PlanUsage.parse(JSONObject("""{"providers":[{"provider":"openai-codex","error":"Token expired","windows":[]}]}""")))
    }

    @Test fun creditsWithoutBalanceKeepUnknownDistinctFromZeroAndUnlimited() {
        val unknown = PlanUsage.parse(JSONObject("""{"providers":[{"provider":"openai-codex","credits":{}}]}"""))!!
        assertNull(unknown.credits!!.balance)
        assertNull(unknown.credits.hasCredits)
        val unlimited = PlanUsage.parse(JSONObject("""{"providers":[{"provider":"openai-codex","credits":{"unlimited":true}}]}"""))!!
        assertEquals(true, unlimited.credits!!.unlimited)
    }
}
