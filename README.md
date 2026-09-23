# Habit Streaks 🔥

An offline habit tracker, to-do list and timer for Android, with a dark look and a hot-pink accent.

- **Today**: a scrollable date strip plus everything scheduled for that day. Tap the circle to check an item off.
- **Habits** can be evaluated four ways:
  - **Yes/no**
  - **Numeric**, e.g. 8 glasses of water
  - **Timer**, e.g. 30 min of reading, with a built-in stopwatch
  - **Checklist** of sub-items
- **Flexible schedules**: every day, specific weekdays, N days per week, specific days of the month, or every N days. Each schedule can have a start date and an optional end date.
- **Step-by-step creation**: category → how to evaluate → define → how often → when (plus reminder and priority).
- **Statistics** for each habit: a monthly calendar, habit score, current and best streak, completion counts and a 6-month bar chart.
- **Tasks**: one-off tasks with a date and priority, plus recurring tasks (they repeat on a schedule but have no stats).
- **Categories**: 15 built-in categories plus your own, each with an icon and color.
- **Timer tab** with a stopwatch and a countdown with presets.
- **Reminders** for each habit, sent only if the habit isn't done yet.
- **Backup & restore** to a JSON file.

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
