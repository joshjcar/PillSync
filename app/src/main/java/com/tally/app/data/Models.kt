package com.tally.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** How a single reminder time is decided. */
enum class ScheduleType { FIXED, MEAL }

/** Meals a dose can be anchored to. */
enum class Meal { BREAKFAST, LUNCH, DINNER, SNACK }

/** Relation of the dose to the anchoring meal. */
enum class MealRelation { BEFORE, WITH, AFTER }

/** Outcome of a scheduled dose. */
enum class DoseStatus { PENDING, TAKEN, SKIPPED }

/**
 * A medicine the user takes. Each medicine owns one or more [ScheduleItem]s that
 * describe when it must be taken.
 */
@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val dosage: String = "",          // e.g. "1 tablet", "5 ml"
    val colorHex: String = "#1D63E8",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    // Stock tracking: when [stockEnabled], [stockCount] doses remain and decrement as
    // doses are taken; [lowStockThreshold] triggers the "running low" warning.
    val stockEnabled: Boolean = false,
    val stockCount: Int = 0,
    val lowStockThreshold: Int = 5
)

/**
 * One reminder time for a medicine.
 *
 * FIXED : rings at [minuteOfDay] (minutes since midnight).
 * MEAL  : rings relative to a configured meal time — [relation] and [offsetMinutes]
 *         before/after [meal] (meal times are configured in Settings).
 *
 * [daysMask] is a 7-bit mask; bit (Calendar.DAY_OF_WEEK - 1) set = active that weekday.
 * 127 (0b1111111) means every day.
 */
@Entity(
    tableName = "schedules",
    foreignKeys = [ForeignKey(
        entity = Medicine::class,
        parentColumns = ["id"],
        childColumns = ["medicineId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("medicineId")]
)
data class ScheduleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val type: ScheduleType = ScheduleType.FIXED,
    val minuteOfDay: Int = 8 * 60,           // used when type == FIXED
    val meal: Meal? = null,                  // used when type == MEAL
    val relation: MealRelation? = null,      // used when type == MEAL
    val offsetMinutes: Int = 30,             // minutes before/after the meal
    val daysMask: Int = 0b1111111,
    val active: Boolean = true
)

/** History of a fired dose so the home screen can show "taken / missed". */
@Entity(
    tableName = "dose_logs",
    indices = [Index("scheduledAt"), Index("medicineId")]
)
data class DoseLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val scheduleId: Long,
    val scheduledAt: Long,
    val status: DoseStatus = DoseStatus.PENDING,
    val actedAt: Long? = null
)

/** A medicine plus its schedules, used by the UI. */
data class MedicineWithSchedules(
    val medicine: Medicine,
    val schedules: List<ScheduleItem>
)

/** A dose-log row joined with its medicine name, for the History screen. */
data class DoseLogWithName(
    @androidx.room.Embedded val log: DoseLog,
    val medicineName: String?
)
