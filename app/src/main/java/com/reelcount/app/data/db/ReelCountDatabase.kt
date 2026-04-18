package com.reelcount.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ScrollEventEntity::class,
        ReelSessionEntity::class,
        DailyStatsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ReelCountDatabase : RoomDatabase() {
    abstract fun scrollEventDao(): ScrollEventDao
    abstract fun reelSessionDao(): ReelSessionDao
    abstract fun dailyStatsDao(): DailyStatsDao
}
