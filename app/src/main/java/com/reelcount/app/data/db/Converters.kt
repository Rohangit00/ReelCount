package com.reelcount.app.data.db

import androidx.room.TypeConverter
import com.reelcount.app.data.model.ScrollClassification
import com.reelcount.app.data.model.TargetApp

class Converters {
    @TypeConverter
    fun fromScrollClassification(value: ScrollClassification): String = value.name

    @TypeConverter
    fun toScrollClassification(value: String): ScrollClassification =
        ScrollClassification.valueOf(value)

    @TypeConverter
    fun fromTargetApp(value: TargetApp): String = value.name

    @TypeConverter
    fun toTargetApp(value: String): TargetApp = TargetApp.valueOf(value)
}
