package com.sentinel.ai.protection.intent

import com.sentinel.ai.protection.intent.file.FileProtectionAgent
import com.sentinel.ai.protection.intent.file.FileScanner
import com.sentinel.ai.protection.intent.link.LinkProtectionAgent
import com.sentinel.ai.protection.intent.link.LinkScanner
import com.sentinel.ai.core.data.ScanRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing bindings for the Intent Protection subsystem.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class IntentProtectionModule {

    @Binds
    @Singleton
    abstract fun bindLinkScanner(
        agent: LinkProtectionAgent
    ): LinkScanner

    @Binds
    @Singleton
    abstract fun bindFileScanner(
        agent: FileProtectionAgent
    ): FileScanner

    @Binds
    @Singleton
    abstract fun bindIntentThreatAnalyzer(
        analyzer: IntentThreatAnalyzerImpl
    ): IntentThreatAnalyzer

    @Binds
    @Singleton
    abstract fun bindScanRepository(
        repository: IntentScanRepository
    ): ScanRepository
}
