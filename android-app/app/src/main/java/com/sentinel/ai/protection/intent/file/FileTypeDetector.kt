package com.sentinel.ai.protection.intent.file

import android.content.ContentResolver
import android.net.Uri

/**
 * File type detector that uses ContentResolver MIME type querying and filename extensions.
 */
object FileTypeDetector {

    fun detect(uri: Uri, contentResolver: ContentResolver? = null): FileType {
        if (uri.scheme?.lowercase() == "content" && contentResolver != null) {
            val mimeType = contentResolver.getType(uri)
            if (mimeType != null) {
                return fromMimeType(mimeType)
            }
        }
        val filename = uri.lastPathSegment ?: uri.path ?: ""
        val ext = filename.substringAfterLast('.', "").lowercase()
        return fromExtension(ext)
    }

    private fun fromMimeType(mimeType: String): FileType {
        val mimeLower = mimeType.lowercase()
        return when {
            mimeLower.startsWith("image/") -> FileType.IMAGE
            mimeLower == "application/pdf" -> FileType.PDF
            mimeLower == "application/zip" || 
                mimeLower == "application/x-zip-compressed" || 
                mimeLower == "application/x-zip" -> FileType.ZIP
            mimeLower == "application/vnd.android.package-archive" -> FileType.APK
            mimeLower == "application/msword" || 
                mimeLower == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> FileType.DOCX
            else -> FileType.UNKNOWN
        }
    }

    private fun fromExtension(ext: String): FileType {
        return when (ext) {
            "pdf" -> FileType.PDF
            "zip", "rar", "7z", "tar", "gz" -> FileType.ZIP
            "apk" -> FileType.APK
            "docx", "doc" -> FileType.DOCX
            "jpg", "jpeg", "png", "webp", "gif", "bmp" -> FileType.IMAGE
            else -> FileType.UNKNOWN
        }
    }
}

/**
 * File type categories supported by SafeX AI.
 */
enum class FileType {
    UNKNOWN,
    PDF,
    ZIP,
    APK,
    IMAGE,
    DOCX,
}
