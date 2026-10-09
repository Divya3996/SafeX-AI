package com.sentinel.ai.protection.floating

import android.app.*
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.sentinel.ai.core.i18n.I18n

internal object FloatingNotifications {
    const val CHANNEL = "safex_floating_status_v1"
    const val BUBBLE_ID = 915010
    const val CAPTURE_ID = 915011
    const val READY_ID = 915012
    fun status(context: Context, capture: Boolean = false): Notification {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL, I18n.translate(context, "Floating assistant status"), NotificationManager.IMPORTANCE_LOW)
        channel.setSound(null, null); channel.enableVibration(false)
        manager.createNotificationChannel(channel)
        val open = PendingIntent.getActivity(context, 1, FloatingAssistantActivity.intent(context, "setup"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(context, 2, Intent(context, if (capture) OneShotScreenCaptureService::class.java else FloatingAssistantService::class.java).setAction("stop"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(com.sentinel.ai.core.R.drawable.safex_notification)
            .setContentTitle(I18n.translate(context, if (capture) "SafeX AI • capturing one screen" else "SafeX AI • floating assistant"))
            .setContentText(I18n.translate(context, if (capture) "Capture stops after one frame. Tap Stop to cancel." else "Tap the shield to choose content. Screen capture requires your approval."))
            .setContentIntent(open).setOngoing(true).setSilent(true).setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .addAction(0, I18n.translate(context, "Stop"), stop).build()
    }
    @android.annotation.SuppressLint("MissingPermission")
    fun ready(context: Context, sessionId: String? = null) {
        if (!com.sentinel.ai.core.feature.WarningChannels.canPost(context)) return
        context.getSystemService(NotificationManager::class.java).notify(READY_ID,
            NotificationCompat.Builder(context, CHANNEL).setSmallIcon(com.sentinel.ai.core.R.drawable.safex_notification)
                .setContentTitle(I18n.translate(context, "Captured screen ready"))
                .setContentText(I18n.translate(context, "Tap to crop and review privately."))
                .setContentIntent(PendingIntent.getActivity(context, 3, FloatingAssistantActivity.intent(context, "review", sessionId), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                .setAutoCancel(true).setSilent(true).setTimeoutAfter(60000).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build())
    }
    @android.annotation.SuppressLint("MissingPermission")
    fun failure(context: Context, message: String, sessionId: String? = null) {
        if (!com.sentinel.ai.core.feature.WarningChannels.canPost(context)) return
        context.getSystemService(NotificationManager::class.java).notify(READY_ID,
            NotificationCompat.Builder(context, CHANNEL).setSmallIcon(com.sentinel.ai.core.R.drawable.safex_notification)
                .setContentTitle(I18n.translate(context, "SafeX AI • floating assistant"))
                .setContentText(I18n.translate(context, message))
                .setContentIntent(PendingIntent.getActivity(context, 3, FloatingAssistantActivity.intent(context, "review", sessionId), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                .setAutoCancel(true).setSilent(true).setTimeoutAfter(60000).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build())
    }
    @android.annotation.SuppressLint("MissingPermission")
    fun warning(context: Context, urgent: Boolean, sessionId: String? = null) {
        val channels = com.sentinel.ai.core.feature.WarningChannels
        if (!channels.canPost(context)) return
        channels.ensure(context)
        context.getSystemService(NotificationManager::class.java).notify(915013,
            NotificationCompat.Builder(context, channels.id(urgent)).setSmallIcon(com.sentinel.ai.core.R.drawable.safex_notification)
                .setContentTitle(I18n.translate(context, "SafeX AI • review your scan"))
                .setContentText(I18n.translate(context, "Your private scan found risk signals. Tap to review."))
                .setContentIntent(PendingIntent.getActivity(context, 4, FloatingAssistantActivity.intent(context, "result", sessionId), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setOnlyAlertOnce(true).setAutoCancel(true).setTimeoutAfter(60000).build())
    }
}
