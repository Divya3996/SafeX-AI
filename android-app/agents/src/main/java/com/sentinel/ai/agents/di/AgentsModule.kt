package com.sentinel.ai.agents.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for agent coordinator bindings.
 * Coordinators are constructor-injected and auto-provided by Hilt.
 */
@Module
@InstallIn(SingletonComponent::class)
object AgentsModule
