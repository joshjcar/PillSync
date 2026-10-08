package com.tally.app.ui

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.core.content.ContextCompat
import com.tally.app.data.Meal
import com.tally.app.data.MealRelation
import com.tally.app.data.ScheduleItem
import com.tally.app.data.ScheduleType
import java.util.Calendar

/** "8:00 AM" style formatting from minutes-since-midnight, honouring the device 12/24h setting. */
fun formatMinute(context: Context, minuteOfDay: Int): String {
    val m = ((minuteOfDay % 1440) + 1440) % 1440
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, m / 60)
        set(Calendar.MINUTE, m % 60)
    }
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return DateFormat.format(pattern, cal).toString()
}

val WEEKDAY_LABELS = listOf("S", "M", "T", "W", "T", "F", "S") // index 0 = Sunday

fun daysSummary(mask: Int): String {
    if (mask and 0b1111111 == 0b1111111) return "Every day"
    if (mask == 0b0111110) return "Weekdays"
    if (mask == 0b1000001) return "Weekends"
    val names = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val on = (0..6).filter { (mask shr it) and 1 == 1 }.map { names[it] }
    return if (on.isEmpty()) "No days" else on.joinToString(", ")
}

/** Human summary of one schedule row, used across the list and detail UIs. */
fun scheduleSummary(context: Context, item: ScheduleItem, resolvedMinute: Int): String {
    val time = formatMinute(context, resolvedMinute)
    return when (item.type) {
        ScheduleType.FIXED -> time
        ScheduleType.MEAL -> {
            val meal = (item.meal ?: Meal.BREAKFAST).name.lowercase().replaceFirstChar { it.uppercase() }
            val rel = when (item.relation ?: MealRelation.WITH) {
                MealRelation.BEFORE -> "${item.offsetMinutes}m before $meal"
                MealRelation.AFTER -> "${item.offsetMinutes}m after $meal"
                MealRelation.WITH -> "with $meal"
            }
            "$rel  (~$time)"
        }
    }
}

fun showTimePicker(context: Context, initialMinute: Int, onPicked: (Int) -> Unit) {
    val m = ((initialMinute % 1440) + 1440) % 1440
    TimePickerDialog(
        context,
        { _, hour, minute -> onPicked(hour * 60 + minute) },
        m / 60,
        m % 60,
        DateFormat.is24HourFormat(context)
    ).show()
}

object Permissions {
    fun hasNotifications(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

    fun canScheduleExact(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
        } else true
}
