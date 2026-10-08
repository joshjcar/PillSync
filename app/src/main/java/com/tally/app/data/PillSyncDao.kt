package com.tally.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PillSyncDao {

    // ---- Medicines ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: Medicine): Long

    @Update
    suspend fun updateMedicine(medicine: Medicine)

    @Delete
    suspend fun deleteMedicine(medicine: Medicine)

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getMedicine(id: Long): Medicine?

    /** Change remaining stock by [delta] (e.g. -1 on a taken dose), floored at 0. No-op if tracking is off. */
    @Query("UPDATE medicines SET stockCount = MAX(stockCount + :delta, 0) WHERE id = :id AND stockEnabled = 1")
    suspend fun adjustStock(id: Long, delta: Int)

    @Query("SELECT * FROM medicines ORDER BY name COLLATE NOCASE ASC")
    fun observeMedicines(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE active = 1")
    suspend fun getActiveMedicines(): List<Medicine>

    @Query("SELECT * FROM medicines")
    suspend fun getAllMedicines(): List<Medicine>

    @Query("DELETE FROM medicines")
    suspend fun deleteAllMedicines()

    // ---- Schedules ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(item: ScheduleItem): Long

    @Update
    suspend fun updateSchedule(item: ScheduleItem)

    @Delete
    suspend fun deleteSchedule(item: ScheduleItem)

    @Query("DELETE FROM schedules WHERE medicineId = :medicineId")
    suspend fun deleteSchedulesForMedicine(medicineId: Long)

    @Query("SELECT * FROM schedules WHERE medicineId = :medicineId")
    suspend fun getSchedulesForMedicine(medicineId: Long): List<ScheduleItem>

    @Query("SELECT * FROM schedules")
    fun observeSchedules(): Flow<List<ScheduleItem>>

    @Query("SELECT * FROM schedules")
    suspend fun getAllSchedules(): List<ScheduleItem>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getSchedule(id: Long): ScheduleItem?

    /** All active schedules that belong to an active medicine — the set of alarms to arm. */
    @Query(
        """
        SELECT s.* FROM schedules s
        INNER JOIN medicines m ON m.id = s.medicineId
        WHERE s.active = 1 AND m.active = 1
        """
    )
    suspend fun getArmableSchedules(): List<ScheduleItem>

    // ---- Dose logs ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoseLog(log: DoseLog): Long

    @Update
    suspend fun updateDoseLog(log: DoseLog)

    @Query("SELECT * FROM dose_logs WHERE scheduleId = :scheduleId AND scheduledAt = :scheduledAt LIMIT 1")
    suspend fun findDoseLog(scheduleId: Long, scheduledAt: Long): DoseLog?

    @Query("SELECT * FROM dose_logs WHERE scheduledAt >= :since ORDER BY scheduledAt DESC")
    fun observeRecentLogs(since: Long): Flow<List<DoseLog>>

    @Query(
        """
        SELECT dl.*, m.name AS medicineName
        FROM dose_logs dl
        LEFT JOIN medicines m ON m.id = dl.medicineId
        WHERE dl.scheduledAt >= :since
        ORDER BY dl.scheduledAt DESC
        """
    )
    fun observeRecentLogsWithName(since: Long): Flow<List<DoseLogWithName>>

    @Query("SELECT * FROM dose_logs WHERE id = :id")
    suspend fun getDoseLog(id: Long): DoseLog?

    @Transaction
    suspend fun replaceMedicine(medicine: Medicine, schedules: List<ScheduleItem>): Long {
        val id = insertMedicine(medicine)
        deleteSchedulesForMedicine(id)
        schedules.forEach { insertSchedule(it.copy(id = 0, medicineId = id)) }
        return id
    }
}
