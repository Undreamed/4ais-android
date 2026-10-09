package com.crsk.openclaw.node

/** Exact, idempotent patch for the pinned runtime's lossy usage mapper. */
object CodexUsagePatch {
    fun apply(source: String, snippet: String): String? {
        if (source.contains("FOURAIS_CODEX_PLAN_USAGE_V1")) return source
        val functionStart = source.indexOf("async function fetchCodexUsage(")
        if (functionStart < 0) return null
        val start = source.indexOf("\tconst windows = [];", functionStart)
        val end = source.indexOf("\n}\n//#endregion", start)
        if (start < 0 || end < start) return null
        val before = source.substring(start, end)
        if (!before.contains("data.rate_limit?.primary_window") ||
            !before.contains("data.credits?.balance") || !before.contains("provider: \"openai-codex\"")) return null
        return source.substring(0, start) + snippet.trimEnd() + source.substring(end)
    }
}
