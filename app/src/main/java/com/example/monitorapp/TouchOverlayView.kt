package com.example.monitorapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.view.View
import android.view.WindowManager

class TouchOverlayView(private val context: Context) {
    private var overlayView: View? = null
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var cursorX = 0f
    private var cursorY = 0f
    private var isCursorPressed = false
    private var isCursorVisible = true
    
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    init {
        createOverlay()
    }
    
    private fun createOverlay() {
        overlayView = object : View(context) {
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                
                if (isCursorVisible) {
                    // Draw cursor
                    paint.color = if (isCursorPressed) Color.RED else Color.BLUE
                    paint.alpha = 180
                    canvas.drawCircle(cursorX, cursorY, 30f, paint)
                    
                    // Draw inner circle
                    paint.color = Color.WHITE
                    paint.alpha = 200
                    canvas.drawCircle(cursorX, cursorY, 15f, paint)
                    
                    // Draw label
                    paint.color = Color.BLACK
                    paint.alpha = 255
                    paint.textSize = 24f
                    val text = if (isCursorPressed) "MONITOR CLICK" else "MONITOR CURSOR"
                    canvas.drawText(text, cursorX + 40, cursorY - 40, paint)
                }
            }
        }
    }
    
    fun showOverlay() {
        try {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            
            overlayView?.let {
                if (it.parent == null) {
                    windowManager.addView(it, params)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun hideOverlay() {
        try {
            overlayView?.let {
                if (it.parent != null) {
                    windowManager.removeView(it)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun updateCursorPosition(x: Float, y: Float) {
        cursorX = x
        cursorY = y
        isCursorVisible = true
        overlayView?.invalidate()
    }
    
    fun setCursorPressed(pressed: Boolean) {
        isCursorPressed = pressed
        overlayView?.invalidate()
    }
    
    fun hideCursor() {
        isCursorVisible = false
        overlayView?.invalidate()
    }
    
    fun destroy() {
        hideOverlay()
    }
}