package com.tally.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-arms every alarm after the device reboots, the clock/timezone changes, or the app updates.
 * This is what makes Tally "always on" with zero user interaction after a restart.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("BootReceiver", "Re-arming alarms after: ${intent.action}")
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AlarmScheduler.rescheduleAll(appContext)
            } catch (t: Throwable) {
                Log.e("BootReceiver", "Reschedule failed", t)
            } finally {
                pending.finish()
            }
        }
    }
}
