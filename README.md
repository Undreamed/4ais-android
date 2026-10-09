<div align="center">

# 📱 4AIs — with ChatGPT

**An AI agent that lives on your Android phone — it sees the screen, taps, types, and gets things done.**

Use your ChatGPT plan or bring your own API key. The agent runs on your phone.

[![CI](https://github.com/Undreamed/4ais-android/actions/workflows/ci.yml/badge.svg)](https://github.com/Undreamed/4ais-android/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B%20arm64-green)

[📥 Releases](https://github.com/Undreamed/4ais-android/releases) · [🔨 Building](docs/BUILDING.md) · [🐛 Report a bug](https://github.com/Undreamed/4ais-android/issues)

<img src="docs/media/demo.gif" alt="The original 4AIs app completing a phone task" width="300">

</div>

A community fork of [4AIs by Sanjay Keerthan](https://github.com/8crsk/openclaw-android),
with the original design and MIT license preserved. This fork adds ChatGPT sign-in
and Android fixes. It isn't an official OpenAI app or a replacement for the original project.

- 🧠 **Pick your brain** — sign in to ChatGPT, or use your own NVIDIA, OpenAI,
  Anthropic or Gemini API key. Your plan's model access and usage limits still apply.
- 📱 **Your phone is the runtime** — [OpenClaw](https://openclaw.ai) runs locally
  inside the app. Model requests go to the provider you choose.
- 👆 **Real phone actions** — read screens, tap, type, swipe and open apps through
  Android Accessibility. No root needed; Shizuku adds optional shell access.
- ✅ **You're in charge** — approvals for sensitive actions and an on-device
  activity log. Reset all app data clears your sign-ins and personal data too.
- 🔌 **Optional integrations and Heartbeat** — connect supported services with
  your own Composio key, or schedule agent tasks.

## 🚀 Get started

1. Download the arm64 APK from [Releases](https://github.com/Undreamed/4ais-android/releases).
2. Install it, open 4AIs and follow setup. Give it Accessibility access and let
   the local runtime install; this needs a network connection and can take a few minutes.
3. Open **Settings → AI provider → ChatGPT**. Tap **Sign in to ChatGPT**, copy
   the one-time code and open the sign-in page. Finish in your browser, then return.
   Or choose an API-key provider and enter your own key.
4. Pick a model and start chatting. Let the agent hatch and introduce itself —
   its name and personality are yours to choose.

GPT-5.5 is the starting choice. GPT-6.1 Sol, GPT-6 Astra, GPT-6 Sol and GPT-6 Luna
are also listed; availability depends on your account. The five were tested with
real replies on a OnePlus 7 Pro. See [what changed and what was tested](docs/FORK.md).

For shell actions, follow [Shizuku's setup guide](https://shizuku.rikka.app/guide/setup/)
and allow 4AIs access. Shizuku needs to be started again after a phone reboot.

**Updating:** this fork keeps the original package name. Android only accepts
updates with the same signing key; an upstream APK can't be replaced by this
fork's APK in place. Development APKs aren't updates for signed releases.

## 🔨 Build it yourself

The source is a normal Android project, with no private overlay or server needed.
See [Building & testing](docs/BUILDING.md). CI builds the APK, runs unit tests and
Android lint, and checks the bundled native code. Signing keys stay private.

## 🤝 Credits

The original app, UI and architecture are by Sanjay Keerthan / [8crsk](https://github.com/8crsk).
This community fork is maintained by [Undreamed](https://github.com/Undreamed).
It uses [OpenClaw](https://github.com/openclaw/openclaw), the upstream Android
Node libraries and [Shizuku](https://github.com/RikkaApps/Shizuku).

[MIT license](LICENSE). Contributions and focused bug reports are welcome.
