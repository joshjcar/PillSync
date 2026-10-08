package com.tally.app.data

import java.util.Calendar

/**
 * Turns a [ScheduleItem] into concrete clock times. Pure functions, no Android deps,
 * so they're trivial to reason about and test.
 */
object ScheduleResolver {

    /** Minutes-since-midnight this schedule should fire at, given current meal settings. */
    fun minuteOfDay(item: ScheduleItem, settings: SettingsStore): Int {
        val raw = when (item.type) {
            ScheduleType.FIXED -> item.minuteOfDay
            ScheduleType.MEAL -> {
                val meal = item.meal ?: Meal.BREAKFAST
                val base = settings.mealMinute(meal)
                when (item.relation ?: MealRelation.WITH) {
                    MealRelation.BEFORE -> base - item.offsetMinutes
                    MealRelation.AFTER -> base + item.offsetMinutes
                    MealRelation.WITH -> base
                }
            }
        }
        // Wrap into a valid 0..1439 range so odd offsets never crash scheduling.
        return ((raw % 1440) + 1440) % 1440
    }

    private fun isDayEnabled(item: ScheduleItem, cal: Calendar): Boolean {
        val bit = cal.get(Calendar.DAY_OF_WEEK) - 1 // SUNDAY(1)->0 .. SATURDAY(7)->6
        return (item.daysMask shr bit) and 1 == 1
    }

    /**
     * Epoch millis of the next time this schedule should fire strictly after [fromMillis].
     * Scans up to 8 days ahead to honour the weekday mask. Returns null if the mask is empty.
     */
    fun nextTrigger(item: ScheduleItem, settings: SettingsStore, fromMillis: Long): Long? {
        if (item.daysMask and 0b1111111 == 0) return null
        val minute = minuteOfDay(item, settings)
        for (dayOffset in 0..8) {
            val candidate = (Calendar.getInstance()).apply {
                timeInMillis = fromMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, minute / 60)
                set(Calendar.MINUTE, minute % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (candidate.timeInMillis > fromMillis && isDayEnabled(item, candidate)) {
                return candidate.timeInMillis
            }
        }
        return null
    }
}
