import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const snippet = fs.readFileSync(new URL('../app/src/main/assets/codex-plan-usage.js', import.meta.url), 'utf8');
const parse = data => vm.runInNewContext(`(() => { ${snippet} })()`, { data, Date });
const plain = data => JSON.parse(JSON.stringify(parse(data)));
const usage = plain({
  plan_type: 'pro',
  rate_limit: {
    primary_window: { used_percent: 27, limit_window_seconds: 18000, reset_at: 1900000000 },
    secondary_window: { used_percent: 64, limit_window_seconds: 604800, reset_at: 1900600000 },
  },
  credits: { has_credits: true, unlimited: false, balance: '42.50' },
  additional_rate_limits: [{ limit_name: 'GPT-6 Astra', rate_limit: {
    primary_window: { used_percent: 91, limit_window_seconds: 18000 },
  } }],
  spend_control: { individual_limit: { remaining_percent: 38, reset_at: 1901000000 } },
  access_token: 'must-never-leave-the-gateway',
});
assert.equal(usage.plan, 'pro');
assert.deepEqual(usage.windows.map(w => [w.label, w.usedPercent]), [
  ['5h', 27], ['Week', 64], ['GPT-6 Astra · 5h', 91], ['Plan budget', 62],
]);
assert.equal(usage.windows[0].resetAt, 1900000000000);
assert.deepEqual(usage.credits, { hasCredits: true, unlimited: false, balance: 42.5 });
assert.ok(!JSON.stringify(usage).includes('must-never'));
assert.deepEqual(plain({ rate_limit: { primary_window: { limit_window_seconds: 18000 } } }).windows, []);
assert.deepEqual(plain({ credits: { balance: null } }).credits, {});
assert.deepEqual(plain({ credits: { balance: 'invalid' } }).credits, {});
assert.deepEqual(plain({ credits: { unlimited: true, balance: null } }).credits, { unlimited: true });
assert.deepEqual(plain({ rate_limit: { primary_window: { used_percent: 120 } } }).windows.map(w => w.usedPercent), [100]);
assert.deepEqual(plain({ additional_rate_limits: null, rate_limit: null }).windows, []);
// Exercise the pinned HTTP fetcher, including its account selection and errors.
const original = fs.readFileSync(new URL('../app/src/test/resources/codex-usage-2026.5.12.js', import.meta.url), 'utf8');
const start = original.indexOf('\tconst windows = [];');
const patched = original.slice(0, start) + snippet + '\n}';
let status = 200;
const fetcher = vm.runInNewContext(`(${patched})`, {
  Date,
  resolveProviderRequestHeaders: () => undefined,
  buildUsageHttpErrorSnapshot: ({ status }) => ({ error: `HTTP ${status}` }),
  fetchJson: async (url, options) => {
    assert.equal(url, 'https://chatgpt.com/backend-api/wham/usage');
    assert.equal(options.headers['ChatGPT-Account-Id'], 'test-account');
    assert.equal(options.headers.Authorization, 'Bearer test-token');
    return { ok: status === 200, status, json: async () => ({ plan_type: 'pro', credits: { balance: '12' } }) };
  },
});
assert.equal((await fetcher('test-token', 'test-account', 5000)).credits.balance, 12);
status = 401;
assert.equal((await fetcher('test-token', 'test-account', 5000)).error, 'HTTP 401');
console.log('Plan usage: limits, credits, missing values and redaction passed');
