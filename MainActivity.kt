package com.example.screenmirrorserver

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {
    private lateinit var tvIpAddress: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvClients: TextView
    private lateinit var btnStartServer: Button
    private lateinit var btnStopServer: Button
    private lateinit var btnStartCapture: Button
    
    private var serverSocket: ServerSocket? = null
    private var isServerRunning = false
    private val connectedClients = mutableListOf<Socket>()
    private val PORT = 5555
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var isCapturing = false
    
    private val SCREEN_CAPTURE_REQUEST = 1001
    private val PERMISSION_REQUEST = 1002
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupListeners()
        displayIpAddress()
        checkPermissions()
    }
    
    private fun initViews() {
        tvIpAddress = findViewById(R.id.tvIpAddress)
        tvStatus = findViewById(R.id.tvStatus)
        tvClients = findViewById(R.id.tvClients)
        btnStartServer = findViewById(R.id.btnStartServer)
        btnStopServer = findViewById(R.id.btnStopServer)
        btnStartCapture = findViewById(R.id.btnStartCapture)
        
        btnStopServer.isEnabled = false
        btnStartCapture.isEnabled = false
    }
    
    private fun setupListeners() {
        btnStartServer.setOnClickListener {
            startServer()
        }
        
        btnStopServer.setOnClickListener {
            stopServer()
        }
        
        btnStartCapture.setOnClickListener {
            requestScreenCapture()
        }
    }
    
    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE
        )
        
        val permissionsNeeded = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), PERMISSION_REQUEST)
        }
    }
    
    private fun displayIpAddress() {
        val ip = getDeviceIpAddress()
        tvIpAddress.text = "Server: $ip:$PORT"
    }
    
    private fun getDeviceIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is InetAddress) {
                        val hostAddress = address.hostAddress
                        if (hostAddress?.indexOf(':') == -1) {
                            return hostAddress ?: "192.168.1.100"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "192.168.1.100"
    }
    
    private fun startServer() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(PORT)
                isServerRunning = true
                
                withContext(Dispatchers.Main) {
                    updateStatus("Server Running ✓")
                    btnStartServer.isEnabled = false
                    btnStopServer.isEnabled = true
                    btnStartCapture.isEnabled = true
                    Toast.makeText(this@MainActivity, "Server started!", Toast.LENGTH_SHORT).show()
                }
                
                while (isServerRunning && isActive) {
                    try {
                        val clientSocket = serverSocket?.accept()
                        clientSocket?.let {
                            connectedClients.add(it)
                            withContext(Dispatchers.Main) {
                                updateClients()
                                Toast.makeText(this@MainActivity, "Client connected!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        if (isServerRunning) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateStatus("Error: ${e.message}")
                }
            }
        }
    }
    
    private fun requestScreenCapture() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager.createScreenCaptureIntent(), SCREEN_CAPTURE_REQUEST)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == SCREEN_CAPTURE_REQUEST) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mediaProjection = projectionManager.getMediaProjection(resultCode, data)
                startScreenCapture()
            } else {
                Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun startScreenCapture() {
        val metrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(metrics)
        
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi
        
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenMirror",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
        
        isCapturing = true
        btnStartCapture.text = "Capturing... ✓"
        btnStartCapture.isEnabled = false
        
        Toast.makeText(this, "Screen capture started!", Toast.LENGTH_SHORT).show()
        
        startStreamingFrames()
    }
    
    private fun startStreamingFrames() {
        lifecycleScope.launch(Dispatchers.IO) {
            while (isCapturing && isActive) {
                try {
                    val image = imageReader?.acquireLatestImage()
                    image?.let {
                        val bitmap = imageToBitmap(it)
                        it.close()
                        
                        bitmap?.let { bmp ->
                            val jpegData = bitmapToJpeg(bmp)
                            sendFrameToClients(jpegData)
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
    
    private fun sendFrameToClients(frameData: ByteArray) {
        val disconnectedClients = mutableListOf<Socket>()
        
        connectedClients.forEach { client ->
            try {
                val output = DataOutputStream(BufferedOutputStream(client.getOutputStream()))
                output.writeInt(frameData.size)
                output.write(frameData)
                output.flush()
            } catch (e: Exception) {
                disconnectedClients.add(client)
            }
        }
        
        // Remove disconnected clients
        if (disconnectedClients.isNotEmpty()) {
            connectedClients.removeAll(disconnectedClients)
            lifecycleScope.launch(Dispatchers.Main) {
                updateClients()
            }
        }
    }
    
    private fun stopServer() {
        isServerRunning = false
        isCapturing = false
        
        lifecycleScope.launch(Dispatchers.IO) {
            connectedClients.forEach { it.close() }
            connectedClients.clear()
            
            serverSocket?.close()
            serverSocket = null
            
            virtualDisplay?.release()
            imageReader?.close()
            mediaProjection?.stop()
            
            withContext(Dispatchers.Main) {
                updateStatus("Server Stopped")
                updateClients()
                btnStartServer.isEnabled = true
                btnStopServer.isEnabled = false
                btnStartCapture.isEnabled = false
                btnStartCapture.text = "Start Screen Capture"
                Toast.makeText(this@MainActivity, "Server stopped", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateStatus(status: String) {
        tvStatus.text = "Status: $status"
    }
    
    private fun updateClients() {
        tvClients.text = "Connected Clients: ${connectedClients.size}"
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopServer()
    }
}
