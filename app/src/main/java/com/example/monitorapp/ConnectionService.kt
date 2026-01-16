package com.example.monitorapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class ConnectionService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var screenCaptureService: ScreenCaptureService? = null
    private var connectionManager: ConnectionManager? = null
    private var commandHandler: CommandHandler? = null
    private var networkStateReceiver: NetworkStateReceiver? = null
    
    private var monitorIp: String? = null
    private var isConnected = false
    
    companion object {
        const val CHANNEL_ID = "MonitorServiceChannel"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        const val ACTION_CONNECTION_STATUS = "com.example.targetphone.CONNECTION_STATUS"
        const val ACTION_TOUCH_EVENT = "com.example.targetphone.TOUCH_EVENT"
        
        const val EXTRA_MONITOR_IP = "EXTRA_MONITOR_IP"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_DATA = "EXTRA_DATA"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        connectionManager = ConnectionManager(this).apply {
            setCallback(object : ConnectionManager.ConnectionCallback {
                override fun onConnected() {
                    isConnected = true
                    broadcastConnectionStatus(true, "Connected to monitor ✓")
                    updateNotification("Connected to monitor ✓")
                }
                
                override fun onDisconnected() {
                    isConnected = false
                    broadcastConnectionStatus(false, "Disconnected from monitor")
                    updateNotification("Disconnected")
                    stopSelf()
                }
                
                override fun onError(error: String) {
                    broadcastConnectionStatus(false, "Error: $error")
                    updateNotification("Error: $error")
                }
            })
        }
        
        commandHandler = CommandHandler(this)
        networkStateReceiver = NetworkStateReceiver()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                monitorIp = intent.getStringExtra(EXTRA_MONITOR_IP)
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Intent>(EXTRA_DATA)
                }
                
                startForeground(NOTIFICATION_ID, createNotification("Starting service..."))
                
                monitorIp?.let { ip ->
                    if (resultCode != -1 && data != null) {
                        startMonitoring(ip, resultCode, data)
                    }
                }
            }
            ACTION_STOP_SERVICE -> {
                stopMonitoring()
                stopSelf()
            }
        }
        
        return START_STICKY
    }
    
    private fun startMonitoring(ip: String, resultCode: Int, data: Intent) {
        serviceScope.launch {
            try {
                updateNotification("Connecting to monitor...")
                
                val connected = connectionManager?.connect(ip) ?: false
                
                if (connected) {
                    screenCaptureService = ScreenCaptureService(this@ConnectionService, resultCode, data)
                    screenCaptureService?.setFrameCallback { frameData ->
                        connectionManager?.sendFrame(frameData)
                    }
                    screenCaptureService?.start()
                    
                    connectionManager?.setTouchEventListener { eventType, x, y ->
                        commandHandler?.handleTouchEvent(eventType, x, y)
                    }
                    
                    updateNotification("Streaming to monitor ✓")
                } else {
                    updateNotification("Failed to connect")
                    stopSelf()
                }
                
            } catch (e: Exception) {
                updateNotification("Error: ${e.message}")
                stopSelf()
            }
        }
    }
    
    private fun stopMonitoring() {
        serviceScope.launch {
            screenCaptureService?.stop()
            connectionManager?.disconnect()
            
            withContext(Dispatchers.Main) {
                broadcastConnectionStatus(false, "Disconnected")
            }
        }
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
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(message: String): Notification {
        val stopIntent = Intent(this, ConnectionService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Target Phone Monitoring")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setColor(Color.BLUE)
            .setOngoing(true)
            .setContentIntent(mainPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )
            .build()
    }
    
    private fun updateNotification(message: String) {
        val notification = createNotification(message)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }
    
    private fun broadcastConnectionStatus(connected: Boolean, message: String) {
        val intent = Intent(ACTION_CONNECTION_STATUS).apply {
            putExtra("isConnected", connected)
            putExtra("message", message)
        }
        sendBroadcast(intent)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        serviceScope.cancel()
    }
}
