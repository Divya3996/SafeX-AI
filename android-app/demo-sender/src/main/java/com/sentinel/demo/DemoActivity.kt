package com.sentinel.demo

import android.app.*
import android.os.Bundle
import android.content.pm.PackageManager
import android.widget.*

/** Separate, network-free fixture app to rehearse actual Android notification capture. */
class DemoActivity : Activity() {
    private val scam = "Bank support: urgent! Share your OTP and password immediately or your account will be blocked."
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 60, 40, 40) }
        layout.addView(TextView(this).apply { text = "SafeX AI notification demo"; textSize = 26f })
        layout.addView(TextView(this).apply { text = "Synthetic fixtures only. This app posts local Android notifications. It does not send messages or use the network."; textSize = 16f })
        layout.addView(Button(this).apply { text = "Post synthetic scam"; setOnClickListener { post(scam) } })
        layout.addView(Button(this).apply { text = "Post safety advice"; setOnClickListener { post("Your payment was successful. Never share your OTP or PIN with anyone.") } })
        setContentView(layout)
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        intent.getStringExtra("test_text")?.let { post(it) }
    }
    private fun post(text: String) {
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Grant notification permission first", Toast.LENGTH_LONG).show(); return
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("fixtures", "Synthetic demo inputs", NotificationManager.IMPORTANCE_DEFAULT))
        manager.notify(System.currentTimeMillis().toInt(), Notification.Builder(this, "fixtures")
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Synthetic demo • test sender")
            .setContentText(text).setStyle(Notification.BigTextStyle().bigText(text)).build())
        Toast.makeText(this, "Synthetic notification posted", Toast.LENGTH_SHORT).show()
    }
}
