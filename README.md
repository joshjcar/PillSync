# PillSync — offline medicine reminder (Android)

PillSync is a **fully self-contained, on-device** medicine reminder. It has **no `INTERNET`
permission**, talks to no server, and never sends your data anywhere. Reminders survive
reboots and re-arm themselves automatically with zero user action.

## Features

- **Create a medicine** with name, dosage, description, and a colour tag.
- **Flexible scheduling per medicine** (add as many as you like):
  - **Fixed time** — e.g. 8:00 AM, on chosen weekdays.
  - **Pre / post-meal** — e.g. *30 min before breakfast*, *with dinner*, *1 h after lunch*.
    Meal times are set once in Settings and every meal reminder follows them.
  - **Dose gapping** — “every 12 h × 2 doses” generates evenly spaced daily reminders.
- **Impossible-to-ignore alarm**: full-screen ringing screen over the lock screen, looping
  alarm tone forced to max volume, **escalating** loudness, and a relentless vibration
  pattern — until you tap **Taken**, **Snooze**, or **Skip**.
- **Notifications** with Taken / Snooze actions and a full-screen intent.
- **Stock countdown** — track remaining doses per medicine with a low-stock "Refill soon"
  warning and a rough "days left" estimate.
- **History** of every dose (Taken / Skipped / Missed), plus a **Take now** button to log an
  ad-hoc dose.
- **Always on**: a boot receiver re-arms every alarm after restart, app update, or time
  change. Exact alarms fire through Doze via `AlarmManager.setAlarmClock`.
- **Automatic on-device backup**: a safety copy is kept in `Downloads/PillSync` so your data
  survives reinstalling or switching phones, with one-tap restore on first run. It never
  leaves the device.
- **First-run privacy consent** and an in-app privacy policy.
- **Safety cap**: an alarm auto-silences after a configurable number of minutes.

## Requirements the user grants once (OS-level, unavoidable)

On first launch the home screen shows banners with one-tap buttons for:
1. **Notifications** (Android 13+).
2. **Exact alarms** (Android 12+) — usually auto-granted because PillSync declares
   `USE_EXACT_ALARM`, but the banner appears if not.
3. **Ignore battery optimization** — recommended so the OS never defers alarms.

These are Android security prompts; no app can bypass them. After granting, nothing else is
ever required — not even after a reboot.

## Build

Open the folder in **Android Studio** (Koala / 2024.1+), let it sync, then Run on a device
or emulator (min Android 8.0 / API 26).

Command line (requires a JDK 17, an Android SDK with platform 34 + build-tools 34, and a
`local.properties` containing `sdk.dir=/path/to/android-sdk`):

```bash
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`.

> Note: `gradle/wrapper/gradle-wrapper.jar` is a binary. Android Studio downloads it
> automatically on first sync; for a clean command-line checkout, regenerate it once with
> `gradle wrapper --gradle-version 8.9`.

## How it works (architecture)

| Layer | Location |
|------|----------|
| Data (Room) | `app/src/main/java/.../data/` — entities, DAO, database, type converters |
| Scheduling logic | `data/ScheduleResolver.kt` (pure next-trigger math) + `alarm/AlarmScheduler.kt` |
| Alarm firing | `alarm/AlarmReceiver.kt` → `alarm/AlarmService.kt` (sound/vibration) + `alarm/AlarmActivity.kt` (full screen) |
| Boot persistence | `alarm/BootReceiver.kt`, plus a re-arm on app process start |
| Backup | `data/BackupManager.kt` (manual export/import) + `data/AutoBackup.kt` (automatic safety copy) |
| Settings | `data/SettingsStore.kt` (SharedPreferences) |
| UI (Compose) | `ui/HomeScreen.kt`, `ui/EditMedicineScreen.kt`, `ui/HistoryScreen.kt`, `ui/SettingsScreen.kt`, `ui/PrivacyScreen.kt` |

Every write goes through the repository layer, which cancels stale OS alarms and arms fresh
ones so what’s on disk always matches what will ring.

## Privacy

PillSync collects no personal data, has no network permission, and keeps everything on the
device. See [PRIVACY_POLICY.md](PRIVACY_POLICY.md) for the full policy (DPDP Act 2023, GDPR,
and CCPA/CPRA).
