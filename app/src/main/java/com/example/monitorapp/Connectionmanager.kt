package com.example.monitorapp

import android.content.Context
import kotlinx.coroutines.*
import java.net.Socket

class ConnectionManager(private val context: Context) {
    
    private var socketClient: SocketClient? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        const val VIDEO_PORT = 5555
        const val TOUCH_PORT = 5557
    }
    
    interface ConnectionCallback {
        fun onConnected()
        fun onDisconnected()
        fun onError(error: String)
    }
    
    private var callback: ConnectionCallback? = null
    
    fun setCallback(callback: ConnectionCallback) {
        this.callback = callback
    }
    
    suspend fun connect(monitorIp: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                socketClient = SocketClient(monitorIp, VIDEO_PORT, TOUCH_PORT)
                val result = socketClient?.connect() ?: false
                
                if (result) {
                    withContext(Dispatchers.Main) {
                        callback?.onConnected()
                    }
                }
                
                result
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback?.onError(e.message ?: "Connection failed")
                }
                false
            }
        }
    }
    
    fun sendFrame(frameData: ByteArray) {
        scope.launch {
            try {
                socketClient?.sendVideoData(frameData)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback?.onError("Failed to send frame: ${e.message}")
                }
            }
        }
    }
    
    fun setTouchEventListener(listener: (Int, Float, Float) -> Unit) {
        socketClient?.setTouchListener(listener)
    }
    
    fun disconnect() {
        scope.launch {
            socketClient?.disconnect()
            withContext(Dispatchers.Main) {
                callback?.onDisconnected()
            }
        }
    }
    
    fun isConnected(): Boolean = socketClient?.isConnected() ?: false
    
    fun cleanup() {
        disconnect()
        scope.cancel()
    }
}