package com.sentinel.ai.core.feature

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sentinel.ai.core.R
import com.sentinel.ai.core.i18n.I18n

/** Real notification audio, controlled by Android channel settings, volume and Do Not Disturb. */
object WarningChannels {
    const val REVIEW = "safex_warning_review_v2"
    const val URGENT = "safex_warning_urgent_v2"
    private const val TEST_ID = 914040
    fun id(highPriority: Boolean) = if (highPriority) URGENT else REVIEW
    fun tone(context: Context): Uri = Uri.parse("android.resource://${context.packageName}/raw/safex_warning")

    fun ensure(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        listOf(false, true).forEach { high ->
            val channelId = id(high)
            val existing = manager.getNotificationChannel(channelId)
            val legacy = manager.getNotificationChannel(if (high) "warning_high" else "warning_medium")
            val name = I18n.translate(context, if (high) "Urgent security warnings" else "Security review warnings")
            val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            val channel = existing ?: NotificationChannel(channelId, name,
                legacy?.importance ?: if (high) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT).apply {
                // Upgrade Android's default tone; preserve earlier silence, custom tones and importance.
                val previousSound = legacy?.sound
                val sound = when {
                    legacy != null && previousSound == null -> null
                    previousSound != null && !RingtoneManager.isDefault(previousSound) -> previousSound
                    else -> tone(context)
                }
                setSound(sound, legacy?.audioAttributes ?: attributes)
                vibrationPattern = legacy?.vibrationPattern ?: longArrayOf(0, 120, 100, 180)
                enableVibration(legacy?.shouldVibrate() ?: true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            }
            channel.name = name
            channel.description = I18n.translate(context, "Warnings use a short SafeX AI tune. Sound and vibration remain under your control.")
            manager.createNotificationChannel(channel)
        }
    }

    fun canPost(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    @android.annotation.SuppressLint("MissingPermission")
    fun test(context: Context): Boolean {
        if (!canPost(context)) return false
        ensure(context)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pending = launch?.let { PendingIntent.getActivity(context, TEST_ID, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) }
        val notification = NotificationCompat.Builder(context, REVIEW)
            .setSmallIcon(R.drawable.safex_notification)
            .setContentTitle(I18n.translate(context, "SafeX AI • warning sound test"))
            .setContentText(I18n.translate(context, "This is a test notification. No threat was detected."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(pending).setAutoCancel(true).setTimeoutAfter(30000).build()
        NotificationManagerCompat.from(context).notify(TEST_ID, notification)
        return true
    }
}
