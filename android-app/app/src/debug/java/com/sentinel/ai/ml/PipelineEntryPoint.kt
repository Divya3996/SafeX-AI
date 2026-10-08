package com.sentinel.ai.ml

import com.sentinel.ai.core.data.ScanRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Debug-only access for native integration checks. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PipelineEntryPoint { fun repository(): ScanRepository }

