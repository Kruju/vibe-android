# Habit Streaks 🔥

A small, offline Android app for building daily habits.

- **Check off habits** with one tap on the Today screen, with a 7-day strip for each habit
- **Streaks**: current streak, best streak and 30-day completion rate
- **Monthly calendar** per habit: tap any past day to fix your history
- **Daily reminder** (optional) that only notifies you when habits are still left
- **Backup**: export and import everything as a JSON file
- Light/dark mode and Material You colors (Android 12+)

All data stays on your phone in a single JSON file in the app's private storage.
There is no account, no network access and no tracking.

## Install on your phone

1. On your phone, open this repo's **Releases** page on GitHub and pick the newest release.
2. Tap `HabitStreaks.apk` to download it.
3. Open the downloaded file. If Android asks, allow your browser or file manager to *install unknown apps*.

Requires Android 8.0 or newer.

## Seamless updates (optional, recommended)

Android only installs an update over an existing app when both are signed with the same key.
Without a key of your own, each CI build is signed with a throwaway debug key. To update, you would then have to
export a backup, uninstall the app, install the new APK and import the backup.

To fix this once, create a key on your computer:

```sh
keytool -genkeypair -keystore habits.keystore -storetype PKCS12 -alias habits \
  -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Habit Streaks"
base64 -w0 habits.keystore   # on macOS: base64 -i habits.keystore
```

Then add two secrets under **Settings → Secrets and variables → Actions**:

| Secret | Value |
| --- | --- |
| `SIGNING_KEYSTORE_BASE64` | the base64 output |
| `SIGNING_PASSWORD` | the password you picked in keytool |

Keep `habits.keystore` somewhere safe, and never commit it.

## Build

Every push runs `.github/workflows/build.yml`, which builds the APK and publishes a GitHub release.

To build locally with the Android SDK installed:

```sh
./gradlew assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```

## Tech

Kotlin, Jetpack Compose and Material 3. There are no database or network libraries: data is stored with `org.json` and `AtomicFile`.
