package com.example.monitorapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.*
import java.net.Socket
import java.nio.ByteBuffer

class ForegroundService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private var videoSocket: Socket? = null
    private var touchSocket: Socket? = null
    private var isConnected = false
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var isCapturing = false
    
    private var monitorIp: String? = null
    private var screenWidth = 0
    private var screenHeight = 0
    
    companion object {
        const val CHANNEL_ID = "MonitorServiceChannel"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        const val ACTION_UPDATE_NOTIFICATION = "ACTION_UPDATE_NOTIFICATION"
        
        const val EXTRA_MONITOR_IP = "EXTRA_MONITOR_IP"
        const val EXTRA_MEDIA_PROJECTION = "EXTRA_MEDIA_PROJECTION"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_DATA = "EXTRA_DATA"
        
        private const val VIDEO_PORT = 5555
        private const val TOUCH_PORT = 5557
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                monitorIp = intent.getStringExtra(EXTRA_MONITOR_IP)
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)
                
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
            ACTION_UPDATE_NOTIFICATION -> {
                val message = intent.getStringExtra("message") ?: "Running..."
                updateNotification(message)
            }
        }
        
        return START_STICKY
    }
    
    private fun startMonitoring(ip: String, resultCode: Int, data: Intent) {
        serviceScope.launch {
            try {
                updateNotification("Connecting to monitor...")
                
                // Connect sockets
                videoSocket = Socket(ip, VIDEO_PORT)
                touchSocket = Socket(ip, TOUCH_PORT)
                isConnected = true
                
                updateNotification("Connected to monitor ✓")
                
                // Initialize media projection
                mediaProjection = MediaProjectionService.getMediaProjection(resultCode, data)
                
                // Start screen capture
                withContext(Dispatchers.Main) {
                    startScreenCapture()
                }
                
                // Start receiving touch events
                startReceivingTouchEvents()
                
            } catch (e: Exception) {
                updateNotification("Connection failed: ${e.message}")
                stopSelf()
            }
        }
    }
    
    private fun startScreenCapture() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        val density = metrics.densityDpi
        
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenMirror",
            screenWidth,
            screenHeight,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
        
        isCapturing = true
        updateNotification("Streaming to monitor ✓")
        
        startStreamingFrames()
    }
    
    private fun startStreamingFrames() {
        serviceScope.launch(Dispatchers.IO) {
            while (isCapturing && isActive && isConnected) {
                try {
                    val image = imageReader?.acquireLatestImage()
                    image?.let {
                        val bitmap = imageToBitmap(it)
                        it.close()
                        
                        bitmap?.let { bmp ->
                            val jpegData = bitmapToJpeg(bmp)
                            sendFrameToMonitor(jpegData)
                        }
                    }
                    delay(100) // ~10 FPS
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun imageToBitmap(image: Image): Bitmap? {
        try {
            val planes = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width
            
            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            
            return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    private fun bitmapToJpeg(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
        return outputStream.toByteArray()
    }
    
    private fun sendFrameToMonitor(frameData: ByteArray) {
        try {
            val output = DataOutputStream(BufferedOutputStream(videoSocket?.getOutputStream()))
            output.writeInt(frameData.size)
            output.write(frameData)
            output.flush()
        } catch (e: Exception) {
            e.printStackTrace()
            stopMonitoring()
        }
    }
    
    private fun startReceivingTouchEvents() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val input = DataInputStream(BufferedInputStream(touchSocket?.getInputStream()))
                
                while (isConnected && isActive) {
                    try {
                        val eventType = input.readByte().toInt()
                        val x = input.readFloat()
                        val y = input.readFloat()
                        
                        // Broadcast touch event to overlay
                        val intent = Intent("com.example.targetphone.TOUCH_EVENT")
                        intent.putExtra("eventType", eventType)
                        intent.putExtra("x", x)
                        intent.putExtra("y", y)
                        sendBroadcast(intent)
                        
                    } catch (e: EOFException) {
                        break
                    } catch (e: Exception) {
                        e.printStackTrace()
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun stopMonitoring() {
        isConnected = false
        isCapturing = false
        
        serviceScope.launch(Dispatchers.IO) {
            videoSocket?.close()
            touchSocket?.close()
            
            virtualDisplay?.release()
            imageReader?.close()
            mediaProjection?.stop()
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
        val stopIntent = Intent(this, ForegroundService::class.java).apply {
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
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        serviceScope.cancel()
    }
}