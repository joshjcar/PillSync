package com.tally.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.tally.app.data.DoseLog
import com.tally.app.data.DoseStatus
import com.tally.app.data.TallyDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires at a scheduled dose time. Immediately starts the ringing [AlarmService], then
 * (a) logs the dose as PENDING and (b) re-arms the *next* occurrence of this schedule.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, -1L)
        val scheduledAt = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, System.currentTimeMillis())
        if (scheduleId <= 0L) return

        // Start the loud alarm right away (foreground service).
        val svc = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START
            putExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, scheduledAt)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(svc)
        } else {
            context.startService(svc)
        }

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = TallyDatabase.get(context).dao()
                val item = dao.getSchedule(scheduleId)
                if (item != null) {
                    val medicine = dao.getMedicine(item.medicineId)
                    // Log this dose as pending if not already recorded.
                    if (dao.findDoseLog(scheduleId, scheduledAt) == null) {
                        dao.insertDoseLog(
                            DoseLog(
                                medicineId = item.medicineId,
                                scheduleId = scheduleId,
                                scheduledAt = scheduledAt,
                                status = DoseStatus.PENDING
                            )
                        )
                    }
                    // Re-arm the next occurrence so the reminder repeats forever.
                    if (item.active && medicine?.active == true) {
                        AlarmScheduler.scheduleNext(context, item, scheduledAt)
                    }
                }
            } catch (t: Throwable) {
                Log.e("AlarmReceiver", "Failed handling alarm", t)
            } finally {
                pending.finish()
            }
        }
    }
}
