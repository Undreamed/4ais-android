// FOURAIS_CODEX_PLAN_USAGE_V1: keep account limits and credits separate.
// Only these public usage fields leave the gateway; no auth or raw response.
const windows = [];
const finite = value => typeof value === 'number' && Number.isFinite(value);
const percent = value => Math.max(0, Math.min(100, value));
const resetTime = window => finite(window?.reset_at) && window.reset_at > 0
  ? window.reset_at * 1000
  : finite(window?.reset_after_seconds) && window.reset_after_seconds > 0
    ? Date.now() + window.reset_after_seconds * 1000 : undefined;
const addWindows = (limit, prefix = '') => {
  for (const key of ['primary_window', 'secondary_window']) {
    const window = limit?.[key];
    if (!finite(window?.used_percent)) continue;
    const seconds = window.limit_window_seconds;
    const label = finite(seconds) && seconds > 0
      ? seconds === 604800 ? 'Week' : seconds === 86400 ? 'Day'
        : `${Math.round(seconds / 3600 * 10) / 10}h`
      : key === 'primary_window' ? 'Current limit' : 'Longer limit';
    windows.push({ label: prefix + label, usedPercent: percent(window.used_percent), resetAt: resetTime(window) });
  }
};
addWindows(data.rate_limit);
if (Array.isArray(data.additional_rate_limits)) {
  for (const extra of data.additional_rate_limits.slice(0, 16)) {
    const name = typeof extra?.limit_name === 'string' ? extra.limit_name.slice(0, 80) : 'Model';
    addWindows(extra?.rate_limit, `${name} · `);
  }
}
const budget = data.spend_control?.individual_limit;
if (finite(budget?.remaining_percent)) {
  windows.push({ label: 'Plan budget', usedPercent: 100 - percent(budget.remaining_percent), resetAt: resetTime(budget) });
}
let credits;
if (data.credits && typeof data.credits === 'object') {
  credits = {};
  if (typeof data.credits.has_credits === 'boolean') credits.hasCredits = data.credits.has_credits;
  if (typeof data.credits.unlimited === 'boolean') credits.unlimited = data.credits.unlimited;
  const raw = data.credits.balance;
  const balance = typeof raw === 'string' && raw.trim() ? Number(raw) : raw;
  if (finite(balance) && balance >= 0) credits.balance = balance;
}
return {
  provider: 'openai-codex', displayName: 'Codex', windows, credits,
  plan: typeof data.plan_type === 'string' ? data.plan_type.slice(0, 40) : undefined,
};
