package com.example.monitorapp

import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.content.Context

object MediaProjectionService {
    
    private var projectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    
    fun initialize(context: Context) {
        projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    
    fun getScreenCaptureIntent(): Intent {
        return projectionManager?.createScreenCaptureIntent() 
            ?: throw IllegalStateException("MediaProjectionManager not initialized")
    }
    
    fun getMediaProjection(resultCode: Int, data: Intent): MediaProjection? {
        mediaProjection?.stop()
        mediaProjection = projectionManager?.getMediaProjection(resultCode, data)
        return mediaProjection
    }
    
    fun stopProjection() {
        mediaProjection?.stop()
        mediaProjection = null
    }
    
    fun isProjecting(): Boolean {
        return mediaProjection != null
    }
}