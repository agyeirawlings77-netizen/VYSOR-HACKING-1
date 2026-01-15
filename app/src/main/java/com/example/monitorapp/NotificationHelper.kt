package com.example.monitorapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID = "MonitorServiceChannel"
        const val NOTIFICATION_ID = 1001
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Running monitoring service"
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
            }
            
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
    
    fun createServiceNotification(
        title: String,
        message: String,
        showStopButton: Boolean = true
    ): Notification {
        val mainIntent = Intent(context, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setColor(Color.BLUE)
            .setOngoing(true)
            .setContentIntent(mainPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        
        if (showStopButton) {
            val stopIntent = Intent(context, ConnectionService::class.java).apply {
                action = ConnectionService.ACTION_STOP_SERVICE
            }
            val stopPendingIntent = PendingIntent.getService(
                context, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )
        }
        
        return builder.build()
    }
    
    fun updateNotification(message: String) {
        val notification = createServiceNotification("Target Phone Monitoring", message)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }
    
    fun cancelNotification() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.cancel(NOTIFICATION_ID)
    }
}