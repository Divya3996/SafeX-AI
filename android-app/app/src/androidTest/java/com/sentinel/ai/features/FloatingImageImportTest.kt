package com.sentinel.ai.features

import android.content.ContentValues
import android.graphics.Bitmap
import android.media.ExifInterface
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import com.sentinel.ai.protection.intent.ImageContentReader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Creates and removes only authored images in the emulator's media store. */
class FloatingImageImportTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private fun image(width: Int, height: Int, test: (android.net.Uri) -> Unit) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "safex-authored-import-${System.nanoTime()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SafeX-test")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            try { context.contentResolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) } }
            finally { bitmap.recycle() }
            test(uri)
        } finally { context.contentResolver.delete(uri, null, null) }
    }
    @Test fun importedPhotoRespectsExifRotationBeforeCropping() {
        image(240, 100) { uri ->
            context.contentResolver.openFileDescriptor(uri, "rw")!!.use { descriptor ->
                val exif = ExifInterface(descriptor.fileDescriptor)
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString()); exif.saveAttributes()
            }
            runBlocking {
                val loaded = ImageContentReader(context).loadWithInfo(uri)
                try { assertEquals(100, loaded.bitmap.width); assertEquals(240, loaded.bitmap.height); assertFalse(loaded.downsampled) }
                finally { loaded.bitmap.recycle() }
            }
        }
    }
    @Test fun importedImageHonorsTheAggregateCallerPixelBudget() {
        image(1080, 2400) { uri -> runBlocking {
            val loaded = ImageContentReader(context).loadWithInfo(uri, maxPixels = 100000)
            try { assertTrue(loaded.downsampled); assertTrue(loaded.bitmap.width.toLong() * loaded.bitmap.height <= 100000) }
            finally { loaded.bitmap.recycle() }
        } }
    }
}
