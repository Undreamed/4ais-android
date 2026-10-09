# About this fork

This is a community fork of [4AIs by Sanjay Keerthan](https://github.com/8crsk/openclaw-android).
The original app, design and MIT license are preserved. This fork adds a way to
use a ChatGPT plan alongside the original bring-your-own-key providers.

## What's different

- **ChatGPT sign-in.** Get a one-time code in the app, finish sign-in in your
  browser and return to 4AIs. OpenClaw saves the login on your phone and handles
  renewal. Browser sign-in is also available.
- **More model choices.** GPT-5.5 is the starting choice, with GPT-6.1 Sol,
  GPT-6 Astra, GPT-6 Sol and GPT-6 Luna also listed. These were available in the
  account used for the device test on October 9, 2026. Access depends on your
  own account and plan; the app doesn't grant extra model access or remove limits.
- **Working model switches.** You can switch between these models without
  restarting the agent.
- **Your plan usage.** Status → Usage shows the signed-in account's remaining
  limits, reset times and extra credits. Additional model limits and a plan
  budget appear when the service provides them. Refresh by tapping the arrow;
  the card also updates once a minute while open. Missing values aren't guessed.
  This is a read-only account request; it doesn't send a chat or consume model
  tokens. Automatic refresh stops when you leave Status or background the app.
- **Android-friendly phone commands.** The agent runs its phone helper through
  Android's shell, so it works with Android's restrictions on executable files.
- **A complete reset.** Settings → Reset all app data uses Android's own reset.
  This removes sign-ins, API keys, chats, settings, agent notes and local reports,
  closes the app and returns to setup when you open it again. Permissions need
  to be granted again. It affects 4AIs; it doesn't reset the separate Shizuku app.

The embedded OpenClaw version remains pinned to `2026.5.12`. A narrow,
version-checked usage mapper preserves extra model limits, plan-budget percentages
and credit balances from its existing `usage.status` request. The original mapper
dropped these fields and labelled credits as dollars. The account schema comes
from [OpenAI's Codex source](https://github.com/openai/codex/tree/main/codex-rs/codex-backend-openapi-models/src/models).
Usage stays in memory and is covered by the app's complete reset.

A version-checked workaround omits an unsupported request field on the ChatGPT
route ([upstream issue](https://github.com/openclaw/openclaw/issues/73963)). The
normal API-key providers keep their existing transport.

Only known error categories are stored in a small local report if ChatGPT
rejects a request. Prompts, answers, authorization headers and tokens are not
written to that report. Temporary debugging scripts, device logs, sign-in
backups and the private experiment's patch history aren't part of this repo.

## What has been tested

The subscription card was tested with a live account on the OnePlus 7 Pro:
remaining weekly usage, reset time, credit balance, manual refresh and recovery
after stopping and starting the gateway. Parser checks cover missing fields,
additional model limits and plan-budget percentages.

The feature version was tested on a OnePlus 7 Pro, Android 12: real ChatGPT
sign-in, streaming replies from all five listed models, model switches, gateway
restarts, Accessibility and Shizuku shell access. A chat instruction opened
Android Settings and returned to 4AIs successfully.

The cleaned fork is checked with unit tests, Android lint, APK signature and
native-library checks. The destructive reset isn't run on the user's configured
phone during development. It delegates to Android's
[documented app-data reset](https://developer.android.com/reference/android/app/ActivityManager#clearApplicationUserData()).

## Shizuku

Most phone actions use Accessibility and don't need Shizuku. For shell actions,
install [Shizuku](https://shizuku.rikka.app/guide/setup/), start it with wireless
debugging and allow 4AIs access. Some OnePlus devices also need the developer
option that disables permission monitoring, as described in Shizuku's setup guide.
Shizuku needs to be started again after a phone reboot unless the device is rooted.

## Updates and signing

This fork keeps the original package name, `com.crsk.openclaw`, because the
bundled native Node runtime is built for it. It cannot sit alongside the original
app. Android only accepts an update signed with the same key as the installed
version. The upstream APK and this fork use different keys.

Keep your signing key private and backed up. GitHub releases are signed from
repository secrets; no signing key or account credentials are committed.
Development APKs from pull requests use a disposable debug key and aren't
update packages for the signed releases.
