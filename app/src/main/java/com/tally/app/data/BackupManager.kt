package com.tally.app.data

import android.content.Context
import com.tally.app.alarm.AlarmScheduler
import org.json.JSONArray
import org.json.JSONObject

/**
 * Exports and restores all app data as a single JSON document. Fully offline — the caller
 * writes/reads the bytes through a user-picked file (Storage Access Framework), so no storage
 * permission is needed and nothing ever leaves the device except where the user chooses.
 */
object BackupManager {
    private const val VERSION = 1

    suspend fun exportJson(context: Context): String {
        val dao = PillSyncDatabase.get(context).dao()
        val settings = SettingsStore(context)
        val root = JSONObject()
        root.put("version", VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val meds = JSONArray()
        dao.getAllMedicines().forEach { m ->
            meds.put(
                JSONObject()
                    .put("id", m.id)
                    .put("name", m.name)
                    .put("description", m.description)
                    .put("dosage", m.dosage)
                    .put("colorHex", m.colorHex)
                    .put("active", m.active)
                    .put("createdAt", m.createdAt)
                    .put("stockEnabled", m.stockEnabled)
                    .put("stockCount", m.stockCount)
                    .put("lowStockThreshold", m.lowStockThreshold)
            )
        }
        root.put("medicines", meds)

        val schedules = JSONArray()
        dao.getAllSchedules().forEach { s ->
            schedules.put(
                JSONObject()
                    .put("medicineId", s.medicineId)
                    .put("type", s.type.name)
                    .put("minuteOfDay", s.minuteOfDay)
                    .put("meal", s.meal?.name ?: JSONObject.NULL)
                    .put("relation", s.relation?.name ?: JSONObject.NULL)
                    .put("offsetMinutes", s.offsetMinutes)
                    .put("daysMask", s.daysMask)
                    .put("active", s.active)
            )
        }
        root.put("schedules", schedules)

        root.put(
            "settings",
            JSONObject()
                .put("breakfast", settings.breakfastMinute)
                .put("lunch", settings.lunchMinute)
                .put("dinner", settings.dinnerMinute)
                .put("snack", settings.snackMinute)
                .put("snooze", settings.snoozeMinutes)
                .put("escalate", settings.escalate)
                .put("autoSilence", settings.autoSilenceMinutes)
        )
        return root.toString(2)
    }

    data class ImportResult(val medicines: Int, val schedules: Int)

    /** Replaces all current data with the contents of [json]. Throws on malformed input. */
    suspend fun importJson(context: Context, json: String): ImportResult {
        val root = JSONObject(json)
        val medsArr = root.optJSONArray("medicines") ?: JSONArray()
        val schedArr = root.optJSONArray("schedules") ?: JSONArray()

        val dao = PillSyncDatabase.get(context).dao()

        // Cancel every currently-armed alarm before we wipe the tables.
        dao.getAllSchedules().forEach { AlarmScheduler.cancel(context, it.id) }
        dao.deleteAllMedicines() // cascades to schedules

        // Insert medicines, remembering old-id -> new-id so schedules can be relinked.
        val idMap = HashMap<Long, Long>()
        var medCount = 0
        for (i in 0 until medsArr.length()) {
            val o = medsArr.getJSONObject(i)
            val oldId = o.optLong("id", 0L)
            val newId = dao.insertMedicine(
                Medicine(
                    id = 0,
                    name = o.optString("name", "Medicine"),
                    description = o.optString("description", ""),
                    dosage = o.optString("dosage", ""),
                    colorHex = o.optString("colorHex", "#0EA5A0"),
                    active = o.optBoolean("active", true),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    stockEnabled = o.optBoolean("stockEnabled", false),
                    stockCount = o.optInt("stockCount", 0),
                    lowStockThreshold = o.optInt("lowStockThreshold", 5)
                )
            )
            if (oldId != 0L) idMap[oldId] = newId
            medCount++
        }

        var schedCount = 0
        for (i in 0 until schedArr.length()) {
            val o = schedArr.getJSONObject(i)
            val newMedId = idMap[o.optLong("medicineId", -1L)] ?: continue
            dao.insertSchedule(
                ScheduleItem(
                    id = 0,
                    medicineId = newMedId,
                    type = ScheduleType.valueOf(o.optString("type", "FIXED")),
                    minuteOfDay = o.optInt("minuteOfDay", 8 * 60),
                    meal = o.optString("meal").takeIf { it.isNotBlank() && it != "null" }?.let { Meal.valueOf(it) },
                    relation = o.optString("relation").takeIf { it.isNotBlank() && it != "null" }?.let { MealRelation.valueOf(it) },
                    offsetMinutes = o.optInt("offsetMinutes", 30),
                    daysMask = o.optInt("daysMask", 0b1111111),
                    active = o.optBoolean("active", true)
                )
            )
            schedCount++
        }

        // Restore settings if present.
        root.optJSONObject("settings")?.let { s ->
            val settings = SettingsStore(context)
            settings.breakfastMinute = s.optInt("breakfast", settings.breakfastMinute)
            settings.lunchMinute = s.optInt("lunch", settings.lunchMinute)
            settings.dinnerMinute = s.optInt("dinner", settings.dinnerMinute)
            settings.snackMinute = s.optInt("snack", settings.snackMinute)
            settings.snoozeMinutes = s.optInt("snooze", settings.snoozeMinutes)
            settings.escalate = s.optBoolean("escalate", settings.escalate)
            settings.autoSilenceMinutes = s.optInt("autoSilence", settings.autoSilenceMinutes)
        }

        AlarmScheduler.rescheduleAll(context)
        return ImportResult(medCount, schedCount)
    }
}
