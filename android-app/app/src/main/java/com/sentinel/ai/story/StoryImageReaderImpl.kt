package com.sentinel.ai.story

import android.content.Context
import android.net.Uri
import com.sentinel.ai.core.story.*
import com.sentinel.ai.protection.intent.ImageContentReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoryImageReaderImpl @Inject constructor(@ApplicationContext context: Context) : StoryImageReader {
    private val reader = ImageContentReader(context)
    private val imports = Mutex()
    override suspend fun read(uri: Uri, qrOnly: Boolean): StoryImageContent = imports.withLock {
        val caller = currentCoroutineContext().job
        withContext(Dispatchers.IO + NonCancellable) {
            // Queued imports remain cancellable before allocating pixels. Native
            // consumers retain the pixel lease until completion, while seeing cancellation.
            caller.ensureActive()
            val image = reader.loadWithInfo(uri, 2048, 2_000_000)
            try {
                caller.ensureActive()
                val extracted = reader.extract(image.bitmap, qrOnly, cancellation = caller)
                require(!extracted.textTruncated && !extracted.qrTruncated)
                val note = if (extracted.partial || image.downsampled || extracted.outcomes.any { it.qualityLimited })
                    "Recognition may be incomplete. Check spelling, missing words and decoded QR content."
                    else "Extracted on this device. Review text and QR content before adding it."
                StoryImageContent(extracted.text, extracted.qrCodes, note)
            } finally { image.bitmap.recycle() }
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class StoryBindings {
    @Binds abstract fun store(value: StoryVault): StoryStore
    @Binds abstract fun images(value: StoryImageReaderImpl): StoryImageReader
}
