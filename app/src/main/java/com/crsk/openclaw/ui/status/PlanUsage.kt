package com.crsk.openclaw.ui.status

import org.json.JSONObject

data class UsageWindow(val label: String, val remainingPercent: Double, val resetAt: Long?)
data class UsageCredits(val hasCredits: Boolean?, val unlimited: Boolean?, val balance: Double?)
data class PlanUsage(
    val plan: String?,
    val windows: List<UsageWindow>,
    val credits: UsageCredits?,
    val updatedAt: Long?,
) {
    companion object {
        fun parse(summary: JSONObject): PlanUsage? {
            val providers = summary.optJSONArray("providers") ?: return null
            val provider = (0 until providers.length()).mapNotNull { providers.optJSONObject(it) }
                .firstOrNull { it.optString("provider") == "openai-codex" } ?: return null
            if (provider.optString("error").isNotBlank()) return null
            val rawWindows = provider.optJSONArray("windows")
            val windows = (0 until (rawWindows?.length() ?: 0)).mapNotNull { index ->
                val window = rawWindows?.optJSONObject(index) ?: return@mapNotNull null
                val used = (window.opt("usedPercent") as? Number)?.toDouble()
                    ?.takeIf { it.isFinite() } ?: return@mapNotNull null
                UsageWindow(
                    window.optString("label", "Limit").take(100),
                    100 - used.coerceIn(0.0, 100.0),
                    window.optLong("resetAt").takeIf { it > 0 },
                )
            }
            val credits = provider.optJSONObject("credits")?.let {
                UsageCredits(
                    it.opt("hasCredits") as? Boolean,
                    it.opt("unlimited") as? Boolean,
                    (it.opt("balance") as? Number)?.toDouble()?.takeIf { value -> value.isFinite() && value >= 0 },
                )
            }
            // Older unpatched runtimes append a guessed dollar balance to the plan.
            val plan = provider.optString("plan").substringBefore(" (").take(40)
                .takeIf { it.matches(Regex("[a-zA-Z][a-zA-Z_ -]*")) }
            return PlanUsage(plan, windows, credits, summary.optLong("updatedAt").takeIf { it > 0 })
        }
    }
}

data class UsageUiState(
    val loading: Boolean = false,
    val usage: PlanUsage? = null,
    val message: String? = null,
)
