# PillSync — Privacy Policy

**Last updated:** 7 October 2026
**Effective date:** 7 October 2026

---

## 1. The short version

PillSync is a medicine-reminder app that runs **entirely on your device**.

- We **do not collect** any personal data.
- We **do not transmit** anything, to anyone. The app has **no internet permission at all**.
- There are **no accounts, no analytics, no advertising, no trackers, and no third-party SDKs**.
- Everything you enter stays in PillSync's private storage on your phone and is deleted when you delete it or uninstall the app.

Because no personal data ever leaves your device and we never receive it, there is nothing for us to sell, share, lose, or misuse.

---

## 2. Who is responsible (Data Fiduciary / Data Controller)

This app is published by **Joshua J Cardoz** ("we", "us", "the developer").

- **Contact / Grievance Officer:** Joshua J Cardoz
- **Email:** joshua.joseph.cardoz@gmail.com

Under India's Digital Personal Data Protection Act, 2023 ("DPDP Act") we are the *Data Fiduciary*; under the EU/UK General Data Protection Regulation ("GDPR") we are the *Data Controller*. In practice, because all processing happens locally on your device under your sole control, **you** hold and control your own data at all times.

---

## 3. Scope

This policy applies to the PillSync mobile application ("the app"). It does not apply to your device's operating system, your file manager, or any location where you choose to copy an exported backup (see Section 8).

---

## 4. Information we do NOT collect

For clarity and to remove any ambiguity, PillSync does **not** collect, request, generate, log, or access any of the following:

- Name, email address, phone number, or any contact detail about you.
- Government identifiers (Aadhaar, PAN, passport, SSN, etc.).
- Precise or approximate location, GPS, or network-based location.
- Device identifiers (IMEI, advertising ID, Android ID), IP address, or MAC address.
- Contacts, call logs, SMS, photos, microphone, or camera.
- Usage analytics, telemetry, crash reports, performance metrics, or behavioural data.
- Advertising identifiers or any data for marketing or profiling.
- Cookies or any web-tracking technology.

The app declares **no `INTERNET` and no network-state permissions** in its manifest, so it is technically incapable of sending data off the device.

---

## 5. Information you enter, and how it is stored

To provide reminders, you may enter and the app stores **only on your device**:

- Medicine names, dosage text, and notes/description you type.
- Reminder schedules (times, meal-relative timings, repeat days).
- Optional stock counts and low-stock thresholds.
- A local history of which scheduled doses were taken, skipped, or missed.
- App settings (meal times, snooze length, alarm preferences).

This information is stored in PillSync's **private, sandboxed application storage**, which the Android operating system isolates from other apps. It is **never transmitted** anywhere and is not readable by other apps. We, the developer, never receive or have access to it.

This content is yours. Treating it as health-related information, we apply the strongest available protection: keeping it entirely local and under your control.

---

## 6. Permissions and why they are used

PillSync requests only permissions needed to deliver reliable reminders. **None of them grant network access or read your personal data.**

| Permission | Purpose |
|---|---|
| Notifications (`POST_NOTIFICATIONS`) | Show the reminder/alarm notification. |
| Exact alarms (`SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`) | Fire reminders at the precise time. |
| Run at startup (`RECEIVE_BOOT_COMPLETED`) | Re-arm your reminders after the phone restarts. |
| Vibrate (`VIBRATE`) | Vibrate during an alarm. |
| Keep awake (`WAKE_LOCK`) | Keep the alarm ringing reliably. |
| Full-screen alarm (`USE_FULL_SCREEN_INTENT`) | Show the alarm over the lock screen. |
| Foreground service (`FOREGROUND_SERVICE`, `…_SPECIAL_USE`) | Keep the alarm sounding until you respond. |

---

## 7. No sharing, no third parties, no transfers

- We do **not** share, disclose, sell, rent, or trade any information with anyone.
- We use **no** third-party processors, sub-processors, cloud services, SDKs, or ad networks.
- There is **no cross-border transfer** of personal data, because no data leaves your device.
- We will only ever disclose data we hold about you — and we hold **none** — so there is nothing that could be compelled by legal process from us.

---

## 8. Backups and the automatic safety copy

PillSync lets you **export** a backup file and **import** it later, and also keeps an optional **automatic safety copy** so your data survives reinstalling the app or moving to a new phone.

- **Manual export/import:** you choose where the file is saved using your device's file picker. The backup contains your medicines, schedules, and settings in plain JSON.
- **Automatic safety backup:** so your data always survives reinstalling the app or switching phones, PillSync automatically writes a copy named `pillsync-backup.json` to your device's **Downloads/PillSync** folder whenever your data changes. This copy **remains on your device and is never uploaded or transmitted** anywhere.
- Because the Downloads folder sits **outside** the app's private sandbox, other apps that hold storage access on your device may be able to read that file. This is the trade-off that makes data recovery possible after an uninstall.
- Any backup you then move to a cloud drive, email, or share is governed by **that** service's terms and privacy policy, not this one.
- We recommend keeping your device protected with a screen lock and encryption, which is standard on modern Android.

We never see, upload, receive, or have access to any of your backup files; everything described here happens locally on your device.

---

## 9. Data security

- Data is stored in the OS-enforced private app sandbox, isolated from other apps.
- No data is transmitted, so it cannot be intercepted in transit.
- We recommend you protect the data further by enabling a device screen lock and device encryption (standard on modern Android).

No method of local storage is 100% immune to a compromised or rooted device; within the constraints of the platform, keeping everything local and offline is the most protective design possible.

---

## 10. Data retention and deletion

- Data persists only until **you** remove it.
- You can delete an individual medicine (which removes its schedules and cancels its alarms), or correct any entry at any time.
- Importing a backup **replaces** all existing data.
- Clearing the app's storage in Android Settings, or **uninstalling** the app, permanently deletes all PillSync data from your device.

We retain **nothing**, because we never receive anything.

---

## 11. Your rights

Because your data lives only on your device, you can exercise most rights **directly and immediately inside the app** — no request to us is needed.

### 11.1 Under India's DPDP Act, 2023
You have the right to: **access** a summary of your data; **correct, complete, and update** it; **erase** it; seek **grievance redressal**; and **nominate** another person to act on your behalf. Access, correction, and erasure are performed in-app (edit, delete, export). For grievances, contact our Grievance Officer in Section 2; we will respond within the timeframe required under the DPDP Act and its rules.

### 11.2 Under the GDPR (EEA/UK)
You have the right to **access, rectification, erasure ("right to be forgotten"), restriction of processing, data portability** (via Export), and to **object** to processing. There is **no automated decision-making or profiling**. As all processing occurs locally under your control and we hold no personal data, these rights are satisfied by the in-app controls; you may still contact us with any question.

### 11.3 Under CCPA/CPRA (California)
You have the right to **know, delete, and correct** personal information and to **opt out of its sale or sharing**. **We do not sell or share personal information**, and we collect none, so there is nothing to opt out of.

### 11.4 Other jurisdictions
Equivalent rights under other applicable laws are honoured by the same local, user-controlled design.

---

## 12. Children's privacy

PillSync is a general-purpose reminder tool and is **not directed at children**. It collects no personal data from anyone, including children, and transmits nothing. Parents or guardians who set up reminders for a child do so locally on their own device. If you believe the app should not be used by a child in your care, simply uninstall it; no data will remain.

---

## 13. No profiling or automated decisions

PillSync does not profile you, score you, or make any automated decisions about you. It simply triggers the reminders you configure.

---

## 14. Medical disclaimer

PillSync is a convenience reminder tool and is **not a medical device**. It does not provide medical advice, diagnosis, or treatment. Reminders and alarms may be delayed or missed due to factors outside the app's control (device powered off, battery optimisation, Do Not Disturb, OS restrictions, hardware faults). **Do not rely on PillSync as the sole means of managing critical medication.** Always follow the instructions of your doctor or pharmacist. The developer is not liable for any missed dose or health outcome.

---

## 15. Changes to this policy

If this policy changes, we will update the "Last updated" date above and, where appropriate, note the change inside the app. Because the app is offline, the authoritative copy of this policy is the one bundled inside the app version you have installed. Continued use after an update constitutes acceptance of the revised policy.

---

## 16. Contact

Questions, requests, or grievances:

**Joshua J Cardoz** — **joshua.joseph.cardoz@gmail.com**

---

*This policy is provided in good faith to accurately describe an application that is designed to collect and transmit no personal data. It is not legal advice. For a commercial release, have it reviewed by a qualified professional in your jurisdiction.*
