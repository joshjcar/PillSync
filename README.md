# PillSync — offline medicine reminder (Android)

> Package id is still `com.tally.app` and the project folder is `Tally`, kept stable so
> installed apps upgrade in place. The user-facing app name is **PillSync**.

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
- **Always on**: a boot receiver re-arms every alarm after restart, app update, or time
  change. Exact alarms fire through Doze via `AlarmManager.setAlarmClock`.
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

Command line (if you have Gradle 8.9 installed to generate the wrapper jar the first time):

```bash
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`.

> Note: `gradle/wrapper/gradle-wrapper.jar` is a binary and is not committed here. Android
> Studio downloads it automatically on first sync; the command above regenerates it for CLI use.

## How it works (architecture)

| Layer | Files |
|------|-------|
| Data (Room) | `data/Models.kt`, `TallyDao.kt`, `TallyDatabase.kt`, `Converters.kt` |
| Scheduling logic | `data/ScheduleResolver.kt` (pure functions), `alarm/AlarmScheduler.kt` |
| Alarm firing | `alarm/AlarmReceiver.kt` → `alarm/AlarmService.kt` (sound/vibration) + `alarm/AlarmActivity.kt` (full screen) |
| Boot persistence | `alarm/BootReceiver.kt`, re-arm in `TallyApp.onCreate` |
| Settings | `data/SettingsStore.kt` (SharedPreferences) |
| UI (Compose) | `ui/HomeScreen.kt`, `ui/EditMedicineScreen.kt`, `ui/SettingsScreen.kt` |

Every write goes through `TallyRepository`, which cancels stale OS alarms and arms fresh ones
so what’s on disk always matches what will ring.
