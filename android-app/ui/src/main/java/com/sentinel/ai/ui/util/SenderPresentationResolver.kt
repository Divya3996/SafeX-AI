package com.sentinel.ai.ui.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

data class SenderPresentation(
    val primaryText: String,
    val secondaryText: String? = null
)

fun resolveSenderPresentation(
    context: Context,
    senderDisplayName: String?,
    senderIdentifier: String?
): SenderPresentation {
    val identifier = senderIdentifier?.trim().takeUnless { it.isNullOrEmpty() }
    if (identifier != null) {
        val contactName = context.lookupContactName(identifier)
        return SenderPresentation(
            primaryText = contactName ?: "Unknown Contact",
            secondaryText = identifier
        )
    }

    val preservedValue = senderDisplayName?.trim().takeUnless { it.isNullOrEmpty() }
    return SenderPresentation(primaryText = preservedValue ?: "Unknown Sender")
}

private fun Context.lookupContactName(identifier: String): String? {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
        return null
    }

    return when {
        EMAIL_REGEX.matches(identifier) -> lookupEmailContactName(identifier)
        PHONE_LOOKUP_REGEX.matches(identifier) -> lookupPhoneContactName(identifier)
        else -> lookupExactDataContactName(identifier)
    }
}

private fun Context.lookupPhoneContactName(identifier: String): String? {
    val uri = android.net.Uri.withAppendedPath(
        ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
        android.net.Uri.encode(identifier)
    )
    return contentResolver.query(
        uri,
        arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
        null,
        null,
        null
    ).useFirstString(ContactsContract.PhoneLookup.DISPLAY_NAME)
}

private fun Context.lookupEmailContactName(identifier: String): String? {
    val uri = android.net.Uri.withAppendedPath(
        ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI,
        android.net.Uri.encode(identifier)
    )
    return contentResolver.query(
        uri,
        arrayOf(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME),
        null,
        null,
        null
    ).useFirstString(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME)
}

private fun Context.lookupExactDataContactName(identifier: String): String? {
    val mimetypes = listOf(
        ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE,
        ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE
    )
    val placeholders = mimetypes.joinToString(",") { "?" }
    return contentResolver.query(
        ContactsContract.Data.CONTENT_URI,
        arrayOf(ContactsContract.Data.DISPLAY_NAME),
        "${ContactsContract.Data.DATA1} = ? AND ${ContactsContract.Data.MIMETYPE} IN ($placeholders)",
        arrayOf(identifier, *mimetypes.toTypedArray()),
        null
    ).useFirstString(ContactsContract.Data.DISPLAY_NAME)
}

private fun Cursor?.useFirstString(columnName: String): String? {
    this ?: return null
    use { cursor ->
        if (!cursor.moveToFirst()) return null
        val columnIndex = cursor.getColumnIndex(columnName)
        if (columnIndex < 0) return null
        return cursor.getString(columnIndex)?.takeIf { it.isNotBlank() }
    }
}

private val EMAIL_REGEX = Regex("""^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$""", RegexOption.IGNORE_CASE)
private val PHONE_LOOKUP_REGEX = Regex("""^[+()\-.\s0-9]{7,}$""")
