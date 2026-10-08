package com.sentinel.ai.core.di

import android.content.Context
import com.sentinel.ai.core.coroutines.DispatcherProvider
import com.sentinel.ai.core.event.ThreatEventBus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideThreatEventBus(): ThreatEventBus = ThreatEventBus()

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DispatcherProvider()

    @Provides
    @Singleton
    fun provideApplicationContext(
        @ApplicationContext context: Context
    ): Context = context
}
