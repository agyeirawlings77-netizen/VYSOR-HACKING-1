package com.example.monitorapp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.*
import java.nio.ByteBuffer

class ScreenCaptureService(
    private val context: Context,
    private val resultCode: Int,
    private val data: Intent
) {
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var isCapturing = false
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var frameCallback: ((ByteArray) -> Unit)? = null
    
    private var screenWidth = 0
    private var screenHeight = 0
    
    fun setFrameCallback(callback: (ByteArray) -> Unit) {
        this.frameCallback = callback
    }
    
    fun start() {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        val density = metrics.densityDpi
        
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        
        val mediaProjection = MediaProjectionService.getMediaProjection(resultCode, data)
        
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
        startCapturing()
    }
    
    private fun startCapturing() {
        scope.launch(Dispatchers.IO) {
            while (isCapturing && isActive) {
                try {
                    val image = imageReader?.acquireLatestImage()
                    image?.let {
                        val bitmap = imageToBitmap(it)
                        it.close()
                        
                        bitmap?.let { bmp ->
                            val jpegData = DataSerializer.bitmapToJpeg(bmp)
                            frameCallback?.invoke(jpegData)
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
        return try {
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
            
            Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun stop() {
        isCapturing = false
        scope.launch(Dispatchers.IO) {
            virtualDisplay?.release()
            imageReader?.close()
            MediaProjectionService.stopProjection()
        }
        scope.cancel()
    }
}