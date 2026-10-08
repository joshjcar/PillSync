package com.tally.app.data

import android.content.Context

/**
 * Simple synchronous settings backed by SharedPreferences so alarm receivers and
 * services can read meal times without coroutines. Fully local to the device.
 */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("tally_settings", Context.MODE_PRIVATE)

    var breakfastMinute: Int
        get() = prefs.getInt(KEY_BREAKFAST, 8 * 60)
        set(v) = prefs.edit().putInt(KEY_BREAKFAST, v).apply()

    var lunchMinute: Int
        get() = prefs.getInt(KEY_LUNCH, 13 * 60)
        set(v) = prefs.edit().putInt(KEY_LUNCH, v).apply()

    var dinnerMinute: Int
        get() = prefs.getInt(KEY_DINNER, 20 * 60)
        set(v) = prefs.edit().putInt(KEY_DINNER, v).apply()

    var snackMinute: Int
        get() = prefs.getInt(KEY_SNACK, 17 * 60)
        set(v) = prefs.edit().putInt(KEY_SNACK, v).apply()

    /** How long a snooze lasts, in minutes. */
    var snoozeMinutes: Int
        get() = prefs.getInt(KEY_SNOOZE, 10)
        set(v) = prefs.edit().putInt(KEY_SNOOZE, v).apply()

    /** Ramp the alarm volume up over the first several seconds. */
    var escalate: Boolean
        get() = prefs.getBoolean(KEY_ESCALATE, true)
        set(v) = prefs.edit().putBoolean(KEY_ESCALATE, v).apply()

    /** Safety cap: auto-silence a ringing alarm after this many minutes if untouched. */
    var autoSilenceMinutes: Int
        get() = prefs.getInt(KEY_AUTOSILENCE, 5)
        set(v) = prefs.edit().putInt(KEY_AUTOSILENCE, v).apply()

    /** True once the user has acknowledged the privacy policy on first launch. Survives updates. */
    var privacyAccepted: Boolean
        get() = prefs.getBoolean(KEY_PRIVACY_ACCEPTED, false)
        set(v) = prefs.edit().putBoolean(KEY_PRIVACY_ACCEPTED, v).apply()

    fun mealMinute(meal: Meal): Int = when (meal) {
        Meal.BREAKFAST -> breakfastMinute
        Meal.LUNCH -> lunchMinute
        Meal.DINNER -> dinnerMinute
        Meal.SNACK -> snackMinute
    }

    companion object {
        private const val KEY_BREAKFAST = "breakfast"
        private const val KEY_LUNCH = "lunch"
        private const val KEY_DINNER = "dinner"
        private const val KEY_SNACK = "snack"
        private const val KEY_SNOOZE = "snooze"
        private const val KEY_ESCALATE = "escalate"
        private const val KEY_AUTOSILENCE = "autosilence"
        private const val KEY_PRIVACY_ACCEPTED = "privacy_accepted"
    }
}
