package com.tally.app.ui

/**
 * In-app privacy policy text (markdown-lite: `## ` = heading, `- ` = bullet, `**x**` = bold).
 * Keep in sync with /PRIVACY_POLICY.md.
 */
const val PRIVACY_POLICY_TEXT: String = """
Last updated: 7 October 2026

## The short version
PillSync runs entirely on your device. We **do not collect** any personal data, and the app has **no internet permission at all** — it cannot send anything anywhere. There are **no accounts, no analytics, no advertising, and no third-party code**. Everything you enter stays in PillSync's private storage and is gone when you delete it or uninstall.

Because no data ever leaves your device and we never receive it, there is nothing for us to sell, share, lose, or misuse.

## Who is responsible
Published by **Joshua J Cardoz**.
Grievance Officer / contact: **Joshua J Cardoz**, **joshua.joseph.cardoz@gmail.com**.
Under India's DPDP Act, 2023 we are the Data Fiduciary; under the GDPR, the Data Controller. In practice all processing is local and under your sole control.

## Information we do NOT collect
- Name, email, phone, or any contact detail.
- Government IDs (Aadhaar, PAN, passport, SSN, etc.).
- Location of any kind.
- Device identifiers, advertising ID, IP or MAC address.
- Contacts, SMS, call logs, photos, microphone, or camera.
- Analytics, telemetry, crash reports, or behavioural data.
- Cookies or any web-tracking technology.

The app declares no INTERNET or network permissions, so it is technically incapable of transmitting data off the device.

## What you enter, and how it is stored
To provide reminders, the app stores only on your device: medicine names, dosage and notes; schedules; optional stock counts; a local history of taken/skipped/missed doses; and your settings. This is kept in PillSync's private, OS-sandboxed storage, isolated from other apps, never transmitted, and never accessible to us. We treat it as health-related information and apply the strongest protection available: keeping it fully local and under your control.

## Permissions and why
None of PillSync's permissions grant network access or read your personal data.
- Notifications: show the reminder.
- Exact alarms: fire reminders at the precise time.
- Run at startup: re-arm reminders after a restart.
- Vibrate, keep-awake, full-screen, foreground service: make the alarm ring reliably until you respond.

## No sharing, no third parties, no transfers
We do not share, sell, rent, or disclose anything. We use no processors, cloud services, SDKs, or ad networks. There is no cross-border transfer, because no data leaves your device. We hold no data about you, so there is nothing that could be compelled from us.

## Backups and automatic safety copy
Export and import are optional and chosen by you. In addition, so that your data always survives reinstalling the app or switching phones, PillSync automatically keeps a safety copy named pillsync-backup.json in your device's **Downloads/PillSync** folder, refreshed when your data changes. This copy stays **on your device and is never uploaded or transmitted** anywhere. Because the Downloads folder sits outside the app's private sandbox, other apps on your device that have storage access may be able to read that file. Any backup you then move to a cloud drive or share is governed by that service's policy, not this one. We never see, receive, or have access to your backups.

## Data security
Your data sits in the OS-enforced private app sandbox and is never transmitted, so it cannot be intercepted in transit. We recommend enabling a device screen lock and device encryption. No local storage is fully immune on a compromised or rooted device; an offline, on-device design is the most protective option the platform allows.

## Retention and deletion
Data persists only until you remove it. Delete a medicine to remove its schedules and cancel its alarms; edit any entry anytime; importing a backup replaces existing data. Clearing the app's storage or uninstalling permanently deletes all PillSync data. We retain nothing.

## Your rights
Because your data lives only on your device, you can exercise most rights directly in the app.
- **India (DPDP Act, 2023):** access, correction, completion, updating, erasure, grievance redressal, and nomination. Access/correction/erasure are done in-app; for grievances, contact the Grievance Officer above and we will respond within the time the Act requires.
- **GDPR (EEA/UK):** access, rectification, erasure, restriction, portability (Export), and objection. No automated decision-making or profiling.
- **CCPA/CPRA (California):** right to know, delete, and correct; we do not sell or share personal information, and collect none.
- **Other regions:** equivalent rights are honoured by the same local, user-controlled design.

## Children's privacy
PillSync is not directed at children and collects no data from anyone. A parent or guardian who sets up reminders for a child does so locally on their own device. Uninstalling removes all data.

## No profiling or automated decisions
PillSync does not profile you or make automated decisions. It simply triggers the reminders you configure.

## Medical disclaimer
PillSync is a convenience reminder tool, not a medical device, and gives no medical advice. Reminders may be delayed or missed due to factors outside the app's control (phone off, battery optimisation, Do Not Disturb, OS limits, hardware faults). Do not rely on PillSync as your only means of managing critical medication. Always follow your doctor or pharmacist. The developer is not liable for any missed dose or health outcome.

## Changes to this policy
If this policy changes, the "Last updated" date is revised and, where appropriate, noted in the app. The copy bundled in your installed version is authoritative. Continued use after an update constitutes acceptance.

## Contact
**Joshua J Cardoz** — **joshua.joseph.cardoz@gmail.com**

This policy describes an app designed to collect and transmit no personal data. It is not legal advice; for a commercial release, have it reviewed by a professional in your jurisdiction.
"""
