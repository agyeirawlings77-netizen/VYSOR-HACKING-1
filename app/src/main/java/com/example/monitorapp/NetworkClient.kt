package com.example.monitorapp

import kotlinx.coroutines.*
import java.io.*
import java.net.Socket
import java.net.SocketTimeoutException

class NetworkClient {
    
    private var videoSocket: Socket? = null
    private var touchSocket: Socket? = null
    private var isConnected = false
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val VIDEO_PORT = 5555
        private const val TOUCH_PORT = 5557
        private const val CONNECTION_TIMEOUT = 10000 // 10 seconds
        private const val READ_TIMEOUT = 30000 // 30 seconds
    }
    
    interface ConnectionCallback {
        fun onConnected()
        fun onDisconnected()
        fun onError(error: String)
        fun onTouchEvent(eventType: Int, x: Float, y: Float)
    }
    
    private var callback: ConnectionCallback? = null
    
    fun setCallback(callback: ConnectionCallback) {
        this.callback = callback
    }
    
    suspend fun connect(monitorIp: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Connect video socket
            videoSocket = Socket().apply {
                soTimeout = READ_TIMEOUT
                connect(java.net.InetSocketAddress(monitorIp, VIDEO_PORT), CONNECTION_TIMEOUT)
            }
            
            // Connect touch socket
            touchSocket = Socket().apply {
                soTimeout = READ_TIMEOUT
                connect(java.net.InetSocketAddress(monitorIp, TOUCH_PORT), CONNECTION_TIMEOUT)
            }
            
            isConnected = true
            
            withContext(Dispatchers.Main) {
                callback?.onConnected()
            }
            
            // Start listening for touch events
            startTouchListener()
            
            true
        } catch (e: SocketTimeoutException) {
            withContext(Dispatchers.Main) {
                callback?.onError("Connection timeout. Check IP address and network.")
            }
            false
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                callback?.onError("Connection failed: ${e.message}")
            }
            false
        }
    }
    
    private fun startTouchListener() {
        scope.launch(Dispatchers.IO) {
            try {
                val input = DataInputStream(BufferedInputStream(touchSocket?.getInputStream()))
                
                while (isConnected && isActive) {
                    try {
                        val eventType = input.readByte().toInt()
                        val x = input.readFloat()
                        val y = input.readFloat()
                        
                        withContext(Dispatchers.Main) {
                            callback?.onTouchEvent(eventType, x, y)
                        }
                        
                    } catch (e: EOFException) {
                        break
                    } catch (e: SocketTimeoutException) {
                        // Timeout is normal, continue listening
                        continue
                    } catch (e: Exception) {
                        e.printStackTrace()
                        break
                    }
                }
                
                // Connection lost
                withContext(Dispatchers.Main) {
                    if (isConnected) {
                        callback?.onDisconnected()
                    }
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback?.onError("Touch listener error: ${e.message}")
                }
            }
        }
    }
    
    suspend fun sendFrame(frameData: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isConnected || videoSocket == null || videoSocket?.isClosed == true) {
                return@withContext false
            }
            
            val output = DataOutputStream(BufferedOutputStream(videoSocket?.getOutputStream()))
            output.writeInt(frameData.size)
            output.write(frameData)
            output.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                callback?.onError("Failed to send frame: ${e.message}")
            }
            disconnect()
            false
        }
    }
    
    fun disconnect() {
        isConnected = false
        scope.launch(Dispatchers.IO) {
            try {
                videoSocket?.close()
                touchSocket?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            withContext(Dispatchers.Main) {
                callback?.onDisconnected()
            }
        }
    }
    
    fun isConnectedToMonitor(): Boolean = isConnected
    
    fun cleanup() {
        disconnect()
        scope.cancel()
    }
}