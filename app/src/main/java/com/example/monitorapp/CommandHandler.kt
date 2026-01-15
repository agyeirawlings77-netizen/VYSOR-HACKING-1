package com.example.monitorapp

import android.content.Context
import android.content.Intent

class CommandHandler(private val context: Context) {
    
    companion object {
        const val CMD_TOUCH_MOVE = 0
        const val CMD_TOUCH_DOWN = 1
        const val CMD_TOUCH_UP = 2
        const val CMD_TOUCH_HIDE = 3
        const val CMD_DISCONNECT = 4
        const val CMD_SCREENSHOT = 5
    }
    
    interface CommandListener {
        fun onTouchCommand(eventType: Int, x: Float, y: Float)
        fun onDisconnectCommand()
        fun onScreenshotCommand()
    }
    
    private var listener: CommandListener? = null
    
    fun setCommandListener(listener: CommandListener) {
        this.listener = listener
    }
    
    fun handleCommand(command: Int, data: ByteArray? = null) {
        when (command) {
            CMD_TOUCH_MOVE, CMD_TOUCH_DOWN, CMD_TOUCH_UP, CMD_TOUCH_HIDE -> {
                data?.let {
                    val touchData = DataSerializer.deserializeTouchEvent(it)
                    touchData?.let { (type, x, y) ->
                        listener?.onTouchCommand(type, x, y)
                        broadcastTouchEvent(type, x, y)
                    }
                }
            }
            CMD_DISCONNECT -> {
                listener?.onDisconnectCommand()
            }
            CMD_SCREENSHOT -> {
                listener?.onScreenshotCommand()
            }
        }
    }
    
    fun handleTouchEvent(eventType: Int, x: Float, y: Float) {
        listener?.onTouchCommand(eventType, x, y)
        broadcastTouchEvent(eventType, x, y)
    }
    
    private fun broadcastTouchEvent(eventType: Int, x: Float, y: Float) {
        val intent = Intent(ConnectionService.ACTION_TOUCH_EVENT).apply {
            putExtra("eventType", eventType)
            putExtra("x", x)
            putExtra("y", y)
        }
        context.sendBroadcast(intent)
    }
    
    fun sendResponse(responseCode: Int, message: String) {
        // Can be extended to send responses back to monitor
    }
}