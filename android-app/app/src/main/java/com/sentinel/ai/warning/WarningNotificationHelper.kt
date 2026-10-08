package com.sentinel.ai.warning

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.Manifest
import com.sentinel.ai.R
import com.sentinel.ai.core.model.ProtectionDecision
import com.sentinel.ai.core.model.ScanResult

class WarningNotificationHelper(private val context: Context) {

    private fun tr(text: String) = com.sentinel.ai.core.i18n.I18n.translate(context, text)

    fun showWarning(result: ScanResult, highPriority: Boolean) {
        val model = result.toWarningUiModel()
        if (model.severity == WarningSeverity.NONE && result.decision != ProtectionDecision.BLOCK) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Skipping notification for ${result.id}: POST_NOTIFICATIONS not granted")
            return
        }

        com.sentinel.ai.core.feature.WarningChannels.ensure(context)

        val alertIntent = ScamWarningActivity.newIntent(context, result)

        val contentIntent = PendingIntent.getActivity(
            context,
            result.id.hashCode(),
            alertIntent,
            pendingIntentFlags()
        )

        val notification = NotificationCompat.Builder(context, channelId(highPriority))
            .setSmallIcon(com.sentinel.ai.core.R.drawable.safex_notification)
            .setContentTitle(tr(model.title))
            .setContentText(tr("Risk Level: ${model.riskLevelLabel}"))
            .setStyle(NotificationCompat.BigTextStyle().bigText(buildBody(model)))
            .setPriority(if (highPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(NotificationCompat.Builder(context, channelId(highPriority))
                .setSmallIcon(com.sentinel.ai.core.R.drawable.safex_notification).setContentTitle(tr("SafeX AI security warning"))
                .setContentText(tr("Unlock your phone to review the details")).build())
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(result.id.hashCode(), notification)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun buildBody(model: WarningUiModel): String {
        val reasons = model.reasons.take(3).joinToString("\n") { "• ${tr(it)}" }
        return tr("Risk Level: ${model.riskLevelLabel}") + "\n" + tr("Risk Score: ${model.riskScore.toInt()}") + "\n\n" + tr("Reasons") + ":\n" + reasons
    }

    private fun channelId(highPriority: Boolean) = com.sentinel.ai.core.feature.WarningChannels.id(highPriority)

    private fun pendingIntentFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    companion object {
        private const val TAG = "WarningNotifications"
    }
}
