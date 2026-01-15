package com.example.monitorapp

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

object DataSerializer {
    
    fun bitmapToJpeg(bitmap: Bitmap, quality: Int = 50): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }
    
    fun serializeTouchEvent(eventType: Int, x: Float, y: Float): ByteArray {
        val buffer = ByteArrayOutputStream()
        val output = java.io.DataOutputStream(buffer)
        output.writeByte(eventType)
        output.writeFloat(x)
        output.writeFloat(y)
        return buffer.toByteArray()
    }
    
    fun deserializeTouchEvent(data: ByteArray): Triple<Int, Float, Float>? {
        return try {
            val input = java.io.DataInputStream(java.io.ByteArrayInputStream(data))
            val eventType = input.readByte().toInt()
            val x = input.readFloat()
            val y = input.readFloat()
            Triple(eventType, x, y)
        } catch (e: Exception) {
            null
        }
    }
    
    fun compressBitmap(bitmap: Bitmap, maxWidth: Int = 1080, maxHeight: Int = 1920): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }
        
        val aspectRatio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        
        if (width > height) {
            newWidth = maxWidth
            newHeight = (maxWidth / aspectRatio).toInt()
        } else {
            newHeight = maxHeight
            newWidth = (maxHeight * aspectRatio).toInt()
        }
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}