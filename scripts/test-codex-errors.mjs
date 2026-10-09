#!/usr/bin/env node
// Synthetic backend responses; no network or account credentials.
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const snippet = fs.readFileSync(path.join(root, 'app/src/main/assets/codex-http-diagnostic.js'), 'utf8');
const AsyncFunction = Object.getPrototypeOf(async function() {}).constructor;
const run = new AsyncFunction('parsedUrl', 'currentInit', 'response', snippet);
const temp = fs.mkdtempSync(path.join(os.tmpdir(), '4ais-error-test-'));
process.env.CODEX_DIAG_PATH = path.join(temp, 'error.json');
const secret = 'PROHIBITED_ACCOUNT_DATA_AND_PROMPT_abc123';

async function probe(url, status, payload) {
  fs.rmSync(process.env.CODEX_DIAG_PATH, { force: true });
  await run(new URL(url), { body: JSON.stringify({ input: secret }) },
    new Response(JSON.stringify(payload), { status }));
  return fs.existsSync(process.env.CODEX_DIAG_PATH)
    ? fs.readFileSync(process.env.CODEX_DIAG_PATH, 'utf8') : null;
}
try {
  const codex = 'https://chatgpt.com/backend-api/codex/responses';
  for (const [detail, hint] of [
    ['Unsupported parameter: metadata', 'rejected_field:metadata'],
    ['This model is not supported for this account', 'model_rejected'],
    ['One of "input" or "previous_response_id" must be provided', 'missing_input'],
    ['Account access not authorized', 'access_rejected'],
    [secret, 'unclassified'],
  ]) {
    const raw = await probe(codex, 400, { detail, echo: secret, error: { code: secret } });
    assert.deepEqual(JSON.parse(raw), { status: 400, hint });
    assert.equal(raw.includes(secret), false, 'Never persist account data or prompts');
  }
  assert.equal(await probe('https://api.openai.com/v1/responses', 400, { detail: secret }), null);
  assert.equal(await probe('https://chatgpt.com.evil.example/backend-api/codex/responses', 400, { detail: secret }), null);
  assert.equal(await probe(codex, 200, { response: secret }), null);
  console.log('PASS: known error categories only; no credentials or chat content persisted');
} finally {
  fs.rmSync(temp, { recursive: true, force: true });
}
