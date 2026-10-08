package com.sentinel.ai.protection.intent.reputation

import com.sentinel.ai.BuildConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReputationModule {
    @Binds @Singleton abstract fun bindManager(impl: ReputationManagerImpl): ReputationManager
    @Binds @IntoSet @Singleton abstract fun bindLocal(impl: LocalReputationProvider): ReputationProvider
    companion object {
        @Provides @Singleton fun config() = ReputationConfig("", "", 1000L)
    }
}
