package com.example.monitorapp

import kotlinx.coroutines.*
import java.io.*
import java.net.Socket
import java.net.SocketTimeoutException

class SocketClient(
    private val host: String,
    private val videoPort: Int,
    private val touchPort: Int
) {
    private var videoSocket: Socket? = null
    private var touchSocket: Socket? = null
    private var isConnectedFlag = false
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var touchListener: ((Int, Float, Float) -> Unit)? = null
    
    companion object {
        private const val CONNECTION_TIMEOUT = 10000
        private const val READ_TIMEOUT = 30000
    }
    
    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            videoSocket = Socket().apply {
                soTimeout = READ_TIMEOUT
                connect(java.net.InetSocketAddress(host, videoPort), CONNECTION_TIMEOUT)
            }
            
            touchSocket = Socket().apply {
                soTimeout = READ_TIMEOUT
                connect(java.net.InetSocketAddress(host, touchPort), CONNECTION_TIMEOUT)
            }
            
            isConnectedFlag = true
            startTouchListener()
            true
        } catch (e: SocketTimeoutException) {
            isConnectedFlag = false
            throw Exception("Connection timeout. Check IP and network.")
        } catch (e: Exception) {
            isConnectedFlag = false
            throw Exception("Connection failed: ${e.message}")
        }
    }
    
    private fun startTouchListener() {
        scope.launch(Dispatchers.IO) {
            try {
                val input = DataInputStream(BufferedInputStream(touchSocket?.getInputStream()))
                
                while (isConnectedFlag && isActive) {
                    try {
                        val eventType = input.readByte().toInt()
                        val x = input.readFloat()
                        val y = input.readFloat()
                        
                        withContext(Dispatchers.Main) {
                            touchListener?.invoke(eventType, x, y)
                        }
                        
                    } catch (e: EOFException) {
                        break
                    } catch (e: SocketTimeoutException) {
                        continue
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
    
    suspend fun sendVideoData(frameData: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isConnectedFlag || videoSocket == null || videoSocket?.isClosed == true) {
                return@withContext false
            }
            
            val output = DataOutputStream(BufferedOutputStream(videoSocket?.getOutputStream()))
            output.writeInt(frameData.size)
            output.write(frameData)
            output.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun setTouchListener(listener: (Int, Float, Float) -> Unit) {
        this.touchListener = listener
    }
    
    fun disconnect() {
        isConnectedFlag = false
        scope.launch(Dispatchers.IO) {
            try {
                videoSocket?.close()
                touchSocket?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun isConnected(): Boolean = isConnectedFlag
    
    fun cleanup() {
        disconnect()
        scope.cancel()
    }
}