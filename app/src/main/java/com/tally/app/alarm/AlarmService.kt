package com.tally.app.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tally.app.MainActivity
import com.tally.app.R
import com.tally.app.data.DoseStatus
import com.tally.app.data.SettingsStore
import com.tally.app.data.TallyDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Foreground service that makes the reminder *impossible to ignore*: it forces the alarm
 * stream to full volume, loops the alarm tone, ramps it up, and vibrates relentlessly until
 * the user taps Taken or Snooze. Also drives the full-screen [AlarmActivity].
 */
class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var previousAlarmVolume: Int = -1

    private val handler = Handler(Looper.getMainLooper())
    private var currentScheduleId = -1L
    private var currentScheduledAt = 0L
    private var ringing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentScheduleId = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, -1L)
                currentScheduledAt = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, System.currentTimeMillis())
                startRinging()
            }
            ACTION_STOP -> {
                markDose(DoseStatus.TAKEN)
                stopEverything()
            }
            ACTION_SNOOZE -> {
                doSnooze()
            }
            ACTION_SKIP -> {
                markDose(DoseStatus.SKIPPED)
                stopEverything()
            }
            else -> stopEverything()
        }
        return START_NOT_STICKY
    }

    private fun startRinging() {
        if (ringing) {
            // Already ringing (e.g. a second dose landed); just refresh the notification.
            postForeground()
            return
        }
        ringing = true
        Notifications.ensureChannels(this)
        acquireWakeLock()
        postForeground()
        launchFullScreen()
        startSound()
        startVibration()
        armAutoSilence()
    }

    private fun postForeground() {
        val notification = buildNotification("Time for your medicine", "Tap to open PillSync")
        // The typed 3-arg form is only strictly required (and type-checked) on Android 14+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                Notifications.ALARM_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(Notifications.ALARM_NOTIFICATION_ID, notification)
        }
        // Load the medicine details and refresh the notification text.
        if (currentScheduleId > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                val dao = TallyDatabase.get(applicationContext).dao()
                val schedule = dao.getSchedule(currentScheduleId)
                val med = schedule?.let { dao.getMedicine(it.medicineId) }
                // Schedule/medicine was deleted or disabled before this fired: don't ring.
                if (schedule == null || med == null || !schedule.active || !med.active) {
                    handler.post { stopEverything() }
                    return@launch
                }
                val title = med.name
                val body = buildString {
                    if (med.dosage.isNotBlank()) append(med.dosage)
                    if (med.description.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(med.description)
                    }
                    if (isEmpty()) append("Time to take your dose")
                }
                handler.post {
                    if (ringing) {
                        val nm = getSystemService(android.app.NotificationManager::class.java)
                        nm.notify(Notifications.ALARM_NOTIFICATION_ID, buildNotification(title, body))
                    }
                }
            }
        }
    }

    private fun buildNotification(title: String, body: String): Notification {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, currentScheduleId)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, currentScheduledAt)
        }
        val fullScreenPi = PendingIntent.getActivity(
            this, 1, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val takenPi = servicePendingIntent(ACTION_STOP, 2)
        val snoozePi = servicePendingIntent(ACTION_SNOOZE, 3)

        return NotificationCompat.Builder(this, Notifications.CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_stat_tally)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPi, true)
            .setContentIntent(fullScreenPi)
            .addAction(0, "Taken", takenPi)
            .addAction(0, "Snooze", snoozePi)
            .build()
    }

    private fun servicePendingIntent(action: String, req: Int): PendingIntent {
        val i = Intent(this, AlarmService::class.java).apply {
            this.action = action
            putExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, currentScheduleId)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, currentScheduledAt)
        }
        return PendingIntent.getService(
            this, req, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun launchFullScreen() {
        val i = Intent(this, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, currentScheduleId)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, currentScheduledAt)
        }
        try {
            startActivity(i)
        } catch (t: Throwable) {
            Log.w(TAG, "Direct activity start blocked; relying on full-screen intent", t)
        }
    }

    private fun startSound() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val am = audioManager!!
        // Force the alarm stream loud, remembering the old value to restore later.
        previousAlarmVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        try {
            am.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)
        } catch (_: SecurityException) { /* DND policy may block; tone still plays */ }

        val uri: Uri = firstAvailableSound()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(Notifications.alarmAudioAttributes)
                setDataSource(this@AlarmService, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "MediaPlayer failed", t)
        }

        // Escalate: start quiet, ramp to full over ~15s so it grows more urgent.
        val escalate = SettingsStore(this).escalate
        if (escalate) {
            mediaPlayer?.setVolume(0.15f, 0.15f)
            var step = 0
            val ramp = object : Runnable {
                override fun run() {
                    step++
                    val v = (0.15f + step * 0.17f).coerceAtMost(1f)
                    mediaPlayer?.setVolume(v, v)
                    if (v < 1f && ringing) handler.postDelayed(this, 2500)
                }
            }
            handler.postDelayed(ramp, 2500)
        } else {
            mediaPlayer?.setVolume(1f, 1f)
        }
    }

    private fun firstAvailableSound(): Uri =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI

    @Suppress("DEPRECATION")
    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        // Insistent buzz: 0.6s on, 0.4s off, forever (repeat at index 0).
        val pattern = longArrayOf(0, 600, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun armAutoSilence() {
        val minutes = SettingsStore(this).autoSilenceMinutes.coerceIn(1, 60)
        handler.postDelayed({
            if (ringing) {
                Log.i(TAG, "Auto-silencing after $minutes min (dose left pending)")
                stopEverything()
            }
        }, minutes * 60_000L)
    }

    private fun doSnooze() {
        val minutes = SettingsStore(this).snoozeMinutes.coerceIn(1, 180)
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        if (currentScheduleId > 0) {
            AlarmScheduler.scheduleSnooze(this, currentScheduleId, currentScheduledAt, triggerAt)
        }
        stopEverything()
    }

    private fun markDose(status: DoseStatus) {
        val sid = currentScheduleId
        val at = currentScheduledAt
        if (sid <= 0) return
        CoroutineScope(Dispatchers.IO).launch {
            val dao = TallyDatabase.get(applicationContext).dao()
            val existing = dao.findDoseLog(sid, at)
            if (existing != null && existing.status != status) {
                dao.updateDoseLog(existing.copy(status = status, actedAt = System.currentTimeMillis()))
                // Decrement remaining stock when a dose is confirmed taken.
                if (status == DoseStatus.TAKEN) dao.adjustStock(existing.medicineId, -1)
            }
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "tally:alarm"
        ).also { it.acquire(6 * 60_000L) }
    }

    private fun stopEverything() {
        ringing = false
        handler.removeCallbacksAndMessages(null)
        try { mediaPlayer?.stop() } catch (_: Throwable) {}
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        // Restore the user's previous alarm volume.
        if (previousAlarmVolume >= 0) {
            try {
                audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, previousAlarmVolume, 0)
            } catch (_: Throwable) {}
        }
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (ringing) stopEverything()
    }

    companion object {
        private const val TAG = "AlarmService"
        const val ACTION_START = "com.tally.app.action.START"
        const val ACTION_STOP = "com.tally.app.action.STOP"
        const val ACTION_SNOOZE = "com.tally.app.action.SNOOZE"
        const val ACTION_SKIP = "com.tally.app.action.SKIP"

        fun stop(context: Context, action: String, scheduleId: Long, scheduledAt: Long) {
            val i = Intent(context, AlarmService::class.java).apply {
                this.action = action
                putExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, scheduleId)
                putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, scheduledAt)
            }
            context.startService(i)
        }
    }
}
