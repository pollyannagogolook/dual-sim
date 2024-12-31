package com.pollyannalab.test.dualsim

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat

class SimStateReceiver : BroadcastReceiver() {
    private lateinit var simStateMonitor: SimStateMonitor

    override fun onReceive(context: Context, intent: Intent?) {
        simStateMonitor = SimStateMonitor(context)
        if (intent != null) {
            if (intent.action == "com.example.CALL_STATE_CHANGED") {

                val simCountry = intent.getStringExtra("SIM_COUNTRY")
                Log.d("SimStateReceiver", "CALL_STATE_CHANGED received with country: $simCountry")
                if (simCountry != null) {
                    sendNotification(context, simCountry)
                }
            }
        }
    }


    private fun sendNotification(context: Context, country: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "incoming_call_channel"

        val channel = NotificationChannel(
            channelId,
            "Incoming Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for incoming calls"
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle("來電通知")
            .setContentText("來電試圖撥打的 sim 卡國家：$country")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
