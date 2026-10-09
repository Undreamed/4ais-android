# Building & testing

## Requirements

- Android Studio (Ladybug or newer) with SDK Platform 35
- JDK 17 (bundled with Android Studio)
- Android NDK 28.1.13356709 and Python 3, for the small sign-in terminal helper
- An **arm64-v8a** device, Android 8.0+ (API 26). The bundled Node.js binary
  is arm64-only — x86_64 emulators will not work.

## Build

```bash
git clone https://github.com/Undreamed/4ais-android.git
cd 4ais-android
./scripts/fetch-node-libs.sh   # downloads the prebuilt Node.js .so files (~33MB, one-time)
python3 scripts/build-pty-login.py --ndk "$ANDROID_HOME/ndk/28.1.13356709"
cp local.properties.example local.properties   # set sdk.dir to your SDK path
./gradlew assembleDebug
```

Or open the project in Android Studio and press Run. **No API keys, no
`.env`, no service accounts** — the build is completely self-contained. Users
choose ChatGPT sign-in or supply their own model API key (Settings → AI provider).

On Windows, use Git Bash for `fetch-node-libs.sh`, then run the Python helper
with your NDK path and use `gradlew.bat`. The Python helper supports Windows,
Linux and macOS. Native libraries aren't copied from a previous APK.

> The on-device Node.js native libraries (`app/src/main/jniLibs/`) are not
> tracked in git — they're hosted as a GitHub Release asset and fetched by
> `scripts/fetch-node-libs.sh` (checksum-verified). The build fails with a
> pointer to that script if they're missing.

## Run on a device

1. Install the debug APK (`app/build/outputs/apk/debug/`).
2. First launch walks through: privacy consent → enable the "4AIs UI
   Automation" accessibility service → automatic node bootstrap. The bootstrap
   needs network (it runs `npm install openclaw` on the phone) and takes ~3
   minutes / ~200MB.
3. Choose ChatGPT and sign in, or enter a provider key in Settings. NVIDIA keys are free at
   [build.nvidia.com](https://build.nvidia.com).

### Useful logs

```bash
adb -s YOUR_DEVICE_SERIAL logcat -s NodeSetup NodeProcess ChatViewModel WsRpcClient ShizukuBridge
```

The gateway's stderr also rolls into `filesDir/logs/gateway.err` on-device.

## Tests

```bash
./gradlew testDebugUnitTest   # JVM unit tests
./gradlew lintDebug           # Android lint
node scripts/test-codex-errors.mjs   # only synthetic responses, no login needed
python3 scripts/check-native-elf.py app/build/outputs/apk/debug/app-debug.apk
```

CI runs both plus `assembleDebug` on every PR (`.github/workflows/ci.yml`).

For signed release builds, set `SIGNING_KEYSTORE_PATH`,
`SIGNING_KEYSTORE_PASSWORD` and `SIGNING_KEY_ALIAS`, then run
`./gradlew assembleRelease`. GitHub's release workflow reads the same values
from repository secrets, with the keystore provided as `SIGNING_KEYSTORE_BASE64`.
Never commit a keystore. Back it up privately: Android requires the same key for updates.

## Gotchas

- **Setup runs once per version.** `node-setup.js` is gated by a
  `.4ais_setup_done_v<N>` marker in `filesDir/home/.openclaw/`. If you change
  node-setup.js, bump `SETUP_VERSION` there **and** the marker name in
  `NodeSetup.kt` (they must match), or your changes won't reach existing
  installs.
- **Provider config is derived.** `NodeProcess.refreshMcpConfig` rewrites
  `models.providers` in openclaw's config.json from the device keystore on
  every gateway start — editing config.json by hand won't stick.
- **Benchmark variant** (`./gradlew assembleBenchmark`) is production-like but
  debug-signed, used by the `:benchmark` macrobenchmark module.
