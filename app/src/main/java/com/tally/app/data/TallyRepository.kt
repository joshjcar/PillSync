package com.tally.app.data

import android.content.Context
import com.tally.app.alarm.AlarmScheduler
import kotlinx.coroutines.flow.Flow

/**
 * Single place that mutates data AND keeps the OS alarms in sync. Every write here cancels
 * stale alarms and arms fresh ones so the schedule on disk always matches what will ring.
 */
class TallyRepository(private val context: Context) {
    private val dao = TallyDatabase.get(context).dao()

    fun observeMedicines(): Flow<List<Medicine>> = dao.observeMedicines()
    fun observeSchedules(): Flow<List<ScheduleItem>> = dao.observeSchedules()
    fun observeRecentLogs(sinceMillis: Long): Flow<List<DoseLog>> = dao.observeRecentLogs(sinceMillis)
    fun observeRecentLogsWithName(sinceMillis: Long): Flow<List<DoseLogWithName>> =
        dao.observeRecentLogsWithName(sinceMillis)

    /** Mark a history row taken/skipped/pending, adjusting stock on any taken<->not-taken change. */
    suspend fun setDoseStatus(logId: Long, status: DoseStatus) {
        val log = dao.getDoseLog(logId) ?: return
        val wasTaken = log.status == DoseStatus.TAKEN
        val willBeTaken = status == DoseStatus.TAKEN
        dao.updateDoseLog(
            log.copy(
                status = status,
                actedAt = if (status == DoseStatus.PENDING) null else System.currentTimeMillis()
            )
        )
        if (!wasTaken && willBeTaken) dao.adjustStock(log.medicineId, -1)
        else if (wasTaken && !willBeTaken) dao.adjustStock(log.medicineId, +1) // undo: restore stock
    }

    /** Record an unscheduled "I took it now" dose and decrement stock. */
    suspend fun logAdhocTaken(medicine: Medicine) {
        dao.insertDoseLog(
            DoseLog(
                medicineId = medicine.id,
                scheduleId = 0,
                scheduledAt = System.currentTimeMillis(),
                status = DoseStatus.TAKEN,
                actedAt = System.currentTimeMillis()
            )
        )
        dao.adjustStock(medicine.id, -1)
    }

    suspend fun getSchedulesForMedicine(medicineId: Long): List<ScheduleItem> =
        dao.getSchedulesForMedicine(medicineId)

    /** Insert or update a medicine together with its full set of schedules. */
    suspend fun saveMedicine(medicine: Medicine, schedules: List<ScheduleItem>): Long {
        // Cancel alarms for whatever schedules currently exist for this medicine.
        if (medicine.id != 0L) {
            dao.getSchedulesForMedicine(medicine.id).forEach { AlarmScheduler.cancel(context, it.id) }
        }
        val id = dao.replaceMedicine(medicine, schedules)
        // Arm alarms for the freshly-written schedules.
        if (medicine.active) {
            dao.getSchedulesForMedicine(id).forEach { AlarmScheduler.scheduleNext(context, it) }
        }
        AutoBackup.maybeWrite(context)
        return id
    }

    suspend fun deleteMedicine(medicine: Medicine) {
        dao.getSchedulesForMedicine(medicine.id).forEach { AlarmScheduler.cancel(context, it.id) }
        dao.deleteMedicine(medicine)
        AutoBackup.maybeWrite(context)
    }

    suspend fun setMedicineActive(medicine: Medicine, active: Boolean) {
        dao.updateMedicine(medicine.copy(active = active))
        val schedules = dao.getSchedulesForMedicine(medicine.id)
        if (active) schedules.forEach { AlarmScheduler.scheduleNext(context, it) }
        else schedules.forEach { AlarmScheduler.cancel(context, it.id) }
        AutoBackup.maybeWrite(context)
    }

    suspend fun getMedicine(id: Long): Medicine? = dao.getMedicine(id)
}
