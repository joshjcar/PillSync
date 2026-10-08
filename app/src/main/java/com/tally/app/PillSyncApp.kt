package com.tally.app

import android.app.Application
import com.tally.app.alarm.AlarmScheduler
import com.tally.app.alarm.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PillSyncApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannels(this)
        // Self-heal: make sure every alarm is armed whenever the app process starts.
        appScope.launch(Dispatchers.IO) {
            runCatching { AlarmScheduler.rescheduleAll(this@PillSyncApp) }
        }
    }
}
