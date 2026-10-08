package com.sentinel.ai.core.di

import android.content.Context
import com.sentinel.ai.core.data.local.SentinelDatabase
import com.sentinel.ai.core.data.local.ThreatDao
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
    fun provideSentinelDatabase(
        @ApplicationContext context: Context
    ): SentinelDatabase = SentinelDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideThreatDao(database: SentinelDatabase): ThreatDao = database.threatDao()
}
