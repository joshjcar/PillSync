package com.tally.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.tally.app.MainActivity
import com.tally.app.data.ScheduleItem
import com.tally.app.data.ScheduleResolver
import com.tally.app.data.SettingsStore
import com.tally.app.data.TallyDatabase

/**
 * Arms and cancels exact OS alarms for schedules. Uses [AlarmManager.setAlarmClock] so the
 * alarm survives Doze / app-standby exactly like the system Clock app's alarms.
 */
object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    const val EXTRA_SCHEDULE_ID = "scheduleId"
    const val EXTRA_MEDICINE_ID = "medicineId"
    const val EXTRA_SCHEDULED_AT = "scheduledAt"

    private fun alarmManager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun firePendingIntent(context: Context, scheduleId: Long, scheduledAt: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.tally.app.ALARM_FIRE"
            // Unique data URI guarantees the extras are not merged/overwritten across schedules.
            data = android.net.Uri.parse("tally://alarm/$scheduleId")
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_SCHEDULED_AT, scheduledAt)
        }
        return PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Schedule the next occurrence for one schedule item. */
    fun scheduleNext(context: Context, item: ScheduleItem, fromMillis: Long = System.currentTimeMillis()) {
        val settings = SettingsStore(context)
        val triggerAt = ScheduleResolver.nextTrigger(item, settings, fromMillis)
        if (triggerAt == null) {
            cancel(context, item.id)
            return
        }
        val am = alarmManager(context)
        val fire = firePendingIntent(context, item.id, triggerAt)

        val showIntent = PendingIntent.getActivity(
            context, item.id.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true
        try {
            if (canExact) {
                am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), fire)
            } else {
                // Fallback still fires through idle, just without the status-bar alarm icon.
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, fire)
            }
            Log.i(TAG, "Armed schedule ${item.id} at $triggerAt (exact=$canExact)")
        } catch (se: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, fire)
            Log.w(TAG, "Exact alarm denied, used inexact for ${item.id}", se)
        }
    }

    fun cancel(context: Context, scheduleId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.tally.app.ALARM_FIRE"
            data = android.net.Uri.parse("tally://alarm/$scheduleId")
        }
        val pi = PendingIntent.getBroadcast(
            context, scheduleId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager(context).cancel(pi)
    }

    /** One-off alarm used by "snooze" — fires once at [triggerAt]. */
    fun scheduleSnooze(context: Context, scheduleId: Long, scheduledAt: Long, triggerAt: Long) {
        val am = alarmManager(context)
        val fire = firePendingIntent(context, scheduleId, scheduledAt)
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, fire)
        } catch (se: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, fire)
        }
    }

    /** Re-arm every active schedule of every active medicine. Called on boot, edits, and app start. */
    suspend fun rescheduleAll(context: Context) {
        val dao = TallyDatabase.get(context).dao()
        val now = System.currentTimeMillis()
        val items = dao.getArmableSchedules()
        items.forEach { scheduleNext(context, it, now) }
        Log.i(TAG, "Rescheduled ${items.size} schedules")
    }
}
