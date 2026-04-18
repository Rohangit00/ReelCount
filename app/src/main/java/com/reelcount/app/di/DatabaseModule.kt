package com.reelcount.app.di

import android.content.Context
import androidx.room.Room
import com.reelcount.app.data.db.*
import com.reelcount.app.data.repository.ScrollRepository
import com.reelcount.app.data.repository.ScrollRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ReelCountDatabase =
        Room.databaseBuilder(
            context,
            ReelCountDatabase::class.java,
            "reelcount.db"
        ).build()

    @Provides
    fun provideScrollEventDao(db: ReelCountDatabase): ScrollEventDao = db.scrollEventDao()

    @Provides
    fun provideReelSessionDao(db: ReelCountDatabase): ReelSessionDao = db.reelSessionDao()

    @Provides
    fun provideDailyStatsDao(db: ReelCountDatabase): DailyStatsDao = db.dailyStatsDao()

    @Provides
    @Singleton
    fun provideScrollRepository(
        scrollEventDao: ScrollEventDao,
        reelSessionDao: ReelSessionDao,
        dailyStatsDao: DailyStatsDao
    ): ScrollRepository = ScrollRepositoryImpl(scrollEventDao, reelSessionDao, dailyStatsDao)
}
