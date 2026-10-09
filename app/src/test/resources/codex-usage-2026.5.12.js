async function fetchCodexUsage(token, accountId, timeoutMs, fetchFn) {
	const defaultHeaders = {
		Authorization: `Bearer ${token}`,
		Accept: "application/json"
	};
	if (accountId) defaultHeaders["ChatGPT-Account-Id"] = accountId;
	const res = await fetchJson("https://chatgpt.com/backend-api/wham/usage", {
		method: "GET",
		headers: resolveProviderRequestHeaders({
			provider: "openai-codex",
			baseUrl: "https://chatgpt.com/backend-api/wham/usage",
			capability: "other",
			transport: "http",
			defaultHeaders
		}) ?? defaultHeaders
	}, timeoutMs, fetchFn);
	if (!res.ok) return buildUsageHttpErrorSnapshot({
		provider: "openai-codex",
		status: res.status,
		tokenExpiredStatuses: [401, 403]
	});
	const data = await res.json();
	const windows = [];
	if (data.rate_limit?.primary_window) {
		const pw = data.rate_limit.primary_window;
		const windowHours = Math.round((pw.limit_window_seconds || 10800) / 3600);
		windows.push({
			label: `${windowHours}h`,
			usedPercent: clampPercent(pw.used_percent || 0),
			resetAt: pw.reset_at ? pw.reset_at * 1e3 : void 0
		});
	}
	if (data.rate_limit?.secondary_window) {
		const sw = data.rate_limit.secondary_window;
		const label = resolveSecondaryWindowLabel({
			windowHours: Math.round((sw.limit_window_seconds || 86400) / 3600),
			primaryResetAt: data.rate_limit?.primary_window?.reset_at,
			secondaryResetAt: sw.reset_at
		});
		windows.push({
			label,
			usedPercent: clampPercent(sw.used_percent || 0),
			resetAt: sw.reset_at ? sw.reset_at * 1e3 : void 0
		});
	}
	let plan = data.plan_type;
	if (data.credits?.balance !== void 0 && data.credits.balance !== null) {
		const balance = typeof data.credits.balance === "number" ? data.credits.balance : Number.parseFloat(data.credits.balance) || 0;
		plan = plan ? `${plan} ($${balance.toFixed(2)})` : `$${balance.toFixed(2)}`;
	}
	return {
		provider: "openai-codex",
		displayName: PROVIDER_LABELS["openai-codex"],
		windows,
		plan
	};
}
//#endregion
