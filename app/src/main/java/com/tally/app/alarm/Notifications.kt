package com.tally.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build

object Notifications {
    const val CHANNEL_ALARM = "tally_alarm"
    const val CHANNEL_INFO = "tally_info"

    const val ALARM_NOTIFICATION_ID = 4201

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)

        val alarm = NotificationChannel(
            CHANNEL_ALARM,
            "Medication alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Full-screen ringing reminders to take medicine."
            enableVibration(true)
            enableLights(true)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            // The looping tone is owned by the service's MediaPlayer, not the channel,
            // so we keep the channel silent to avoid a second, non-looping sound.
            setSound(null, null)
        }
        val info = NotificationChannel(
            CHANNEL_INFO,
            "Status",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Quiet status messages." }

        nm.createNotificationChannel(alarm)
        nm.createNotificationChannel(info)
    }

    val alarmAudioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
}
