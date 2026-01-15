package com.example.monitorapp  // ✅ FIXED PACKAGE

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvPermissionStatus: TextView
    private lateinit var tvMonitorIp: TextView
    private lateinit var etMonitorIp: EditText
    private lateinit var btnConnect: Button
    private lateinit var btnDisconnect: Button
    private lateinit var switchShowActions: Switch
    private lateinit var tvDeviceInfo: TextView

    private lateinit var connectionManager: ConnectionManager
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var deviceInfoProvider: DeviceInfoProvider
    private var touchOverlay: TouchOverlayView? = null

    private var showMonitorActions = true
    private var allowMonitorControl = false

    private val SCREEN_CAPTURE_REQUEST = 1001

    private val connectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ConnectionService.ACTION_CONNECTION_STATUS -> {
                    val isConnected = intent.getBooleanExtra("isConnected", false)
                    val message = intent.getStringExtra("message") ?: ""
                    updateConnectionStatus(isConnected, message)
                }
                ConnectionService.ACTION_TOUCH_EVENT -> {
                    if (showMonitorActions) {
                        val eventType = intent.getIntExtra("eventType", 0)
                        val x = intent.getFloatExtra("x", 0f)
                        val y = intent.getFloatExtra("y", 0f)
                        handleTouchEvent(eventType, x, y)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeHelpers()
        initViews()
        setupListeners()
        registerReceivers()
        checkPermissions()
        showInitialPermissionDialog()
    }

    private fun initializeHelpers() {
        connectionManager = ConnectionManager(this)
        permissionHelper = PermissionHelper(this)
        notificationHelper = NotificationHelper(this)
        deviceInfoProvider = DeviceInfoProvider(this)
        MediaProjectionService.initialize(this)
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvPermissionStatus = findViewById(R.id.tvPermissionStatus)
        tvMonitorIp = findViewById(R.id.tvMonitorIp)
        etMonitorIp = findViewById(R.id.etMonitorIp)
        btnConnect = findViewById(R.id.btnConnect)
        btnDisconnect = findViewById(R.id.btnDisconnect)
        switchShowActions = findViewById(R.id.switchShowActions)
        tvDeviceInfo = findViewById(R.id.tvDeviceInfo)

        btnDisconnect.isEnabled = false
        switchShowActions.isChecked = true

        tvDeviceInfo.text = "Device: ${deviceInfoProvider.getDeviceModel()}\n" +
                            "Android: ${deviceInfoProvider.getAndroidVersion()}\n" +
                            "IP: ${deviceInfoProvider.getLocalIpAddress()}"

        updatePermissionStatus()
    }

    private fun setupListeners() {
        btnConnect.setOnClickListener {
            val monitorIp = etMonitorIp.text.toString()
            if (monitorIp.isNotEmpty()) {
                if (permissionHelper.hasAllPermissions()) {
                    requestScreenCaptureAndConnect(monitorIp)
                } else {
                    permissionHelper.requestAllPermissions()
                }
            } else {
                Toast.makeText(this, "Please enter Monitor IP", Toast.LENGTH_SHORT).show()
            }
        }

        btnDisconnect.setOnClickListener { disconnect() }

        switchShowActions.setOnCheckedChangeListener { _, isChecked ->
            showMonitorActions = isChecked
            if (isChecked) {
                touchOverlay?.showOverlay()
                Toast.makeText(this, "Monitor actions now VISIBLE", Toast.LENGTH_SHORT).show()
            } else {
                touchOverlay?.hideOverlay()
                Toast.makeText(this, "Monitor actions now HIDDEN", Toast.LENGTH_SHORT).show()
            }
            updatePermissionStatus()
        }
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(ConnectionService.ACTION_CONNECTION_STATUS)
            addAction(ConnectionService.ACTION_TOUCH_EVENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(connectionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(connectionReceiver, filter)
        }
    }

    private fun showInitialPermissionDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("🔒 Monitor Permission Request")
            .setMessage("A monitor device wants to:\n\n✓ View your screen\n✓ Send touch commands\n\nAllow monitor to control WITHOUT showing actions?")
            .setPositiveButton("Yes, Allow Hidden Control") { dialog, _ ->
                allowMonitorControl = true
                showMonitorActions = false
                switchShowActions.isChecked = false
                Toast.makeText(this, "⚠️ Monitor has hidden control", Toast.LENGTH_LONG).show()
                updatePermissionStatus()
                dialog.dismiss()
            }
            .setNegativeButton("No, Show Me Everything") { dialog, _ ->
                allowMonitorControl = false
                showMonitorActions = true
                switchShowActions.isChecked = true
                Toast.makeText(this, "✓ You will see all actions", Toast.LENGTH_LONG).show()
                updatePermissionStatus()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun updatePermissionStatus() {
        val statusText = when {
            allowMonitorControl && !showMonitorActions -> "⚠️ MONITOR HAS HIDDEN CONTROL"
            showMonitorActions -> "✓ MONITOR ACTIONS VISIBLE"
            else -> "❌ MONITOR ACTIONS HIDDEN"
        }
        tvPermissionStatus.text = statusText
        tvPermissionStatus.setTextColor(
            if (allowMonitorControl && !showMonitorActions) 
                getColor(android.R.color.holo_red_dark)
            else 
                getColor(android.R.color.holo_green_dark)
        )
    }

    private fun checkPermissions() {
        if (!permissionHelper.hasAllPermissions()) {
            permissionHelper.requestAllPermissions()
        }

        if (!permissionHelper.hasOverlayPermission()) {
            OverlayPermissionHelper.requestOverlayPermission(this)
        } else {
            initializeTouchOverlay()
        }
    }

    private fun initializeTouchOverlay() {
        touchOverlay = TouchOverlayView(this)
        if (showMonitorActions) touchOverlay?.showOverlay()
    }

    private fun requestScreenCaptureAndConnect(monitorIp: String) {
        val permissionIntent = Intent(this, ScreenCapturePermissionActivity::class.java)
        permissionIntent.putExtra("monitor_ip", monitorIp)
        startActivityForResult(permissionIntent, SCREEN_CAPTURE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            SCREEN_CAPTURE_REQUEST -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val resultCode = data.getIntExtra(ScreenCapturePermissionActivity.EXTRA_RESULT_CODE, -1)
                    val resultData = data.getParcelableExtra<Intent>(ScreenCapturePermissionActivity.EXTRA_DATA)
                    val monitorIp = etMonitorIp.text.toString()

                    if (resultCode != -1 && resultData != null) {
                        startMonitoringService(monitorIp, resultCode, resultData)
                    }
                } else {
                    Toast.makeText(this, "Screen capture denied", Toast.LENGTH_SHORT).show()
                }
            }
            OverlayPermissionHelper.OVERLAY_PERMISSION_REQUEST_CODE -> {
                if (permissionHelper.hasOverlayPermission()) {
                    initializeTouchOverlay()
                    Toast.makeText(this, "Overlay permission granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startMonitoringService(monitorIp: String, resultCode: Int, data: Intent) {
        val serviceIntent = Intent(this, ConnectionService::class.java).apply {
            action = ConnectionService.ACTION_START_SERVICE
            putExtra(ConnectionService.EXTRA_MONITOR_IP, monitorIp)
            putExtra(ConnectionService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ConnectionService.EXTRA_DATA, data)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent)
        else startService(serviceIntent)

        btnConnect.isEnabled = false
        btnDisconnect.isEnabled = true
        etMonitorIp.isEnabled = false
        updateStatus("Starting service...")
    }

    private fun disconnect() {
        val serviceIntent = Intent(this, ConnectionService::class.java).apply {
            action = ConnectionService.ACTION_STOP_SERVICE
        }
        startService(serviceIntent)

        touchOverlay?.hideOverlay()
        btnConnect.isEnabled = true
        btnDisconnect.isEnabled = false
        etMonitorIp.isEnabled = true
        updateStatus("Disconnected")
    }

    private fun updateConnectionStatus(isConnected: Boolean, message: String) {
        updateStatus(message)
        btnConnect.isEnabled = !isConnected
        btnDisconnect.isEnabled = isConnected
    }

    private fun updateStatus(status: String) {
        tvStatus.text = "Status: $status"
    }

    private fun handleTouchEvent(eventType: Int, x: Float, y: Float) {
        when (eventType) {
            0 -> { touchOverlay?.updateCursorPosition(x, y); touchOverlay?.setCursorPressed(false) }
            1 -> { touchOverlay?.updateCursorPosition(x, y); touchOverlay?.setCursorPressed(true) }
            2 -> { touchOverlay?.updateCursorPosition(x, y); touchOverlay?.setCursorPressed(false) }
            3 -> touchOverlay?.hideCursor()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper.handlePermissionResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(connectionReceiver) } catch (e: Exception) { e.printStackTrace() }
        touchOverlay?.destroy()
        disconnect()
    }
}
