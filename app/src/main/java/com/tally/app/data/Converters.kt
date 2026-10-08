package com.tally.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun scheduleTypeToString(v: ScheduleType): String = v.name
    @TypeConverter fun stringToScheduleType(v: String): ScheduleType = ScheduleType.valueOf(v)

    @TypeConverter fun mealToString(v: Meal?): String? = v?.name
    @TypeConverter fun stringToMeal(v: String?): Meal? = v?.let { Meal.valueOf(it) }

    @TypeConverter fun relationToString(v: MealRelation?): String? = v?.name
    @TypeConverter fun stringToRelation(v: String?): MealRelation? = v?.let { MealRelation.valueOf(it) }

    @TypeConverter fun statusToString(v: DoseStatus): String = v.name
    @TypeConverter fun stringToStatus(v: String): DoseStatus = DoseStatus.valueOf(v)
}
