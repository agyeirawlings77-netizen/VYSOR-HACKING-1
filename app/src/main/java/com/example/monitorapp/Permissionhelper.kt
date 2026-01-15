package com.example.monitorapp

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionHelper(private val activity: Activity) {
    
    companion object {
        const val PERMISSION_REQUEST_CODE = 1002
        
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE
        )
        
        private val OPTIONAL_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }
    }
    
    fun hasAllPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(activity)
        } else {
            true
        }
    }
    
    fun requestAllPermissions() {
        val permissionsNeeded = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                (permissionsNeeded + OPTIONAL_PERMISSIONS).toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] != PackageManager.PERMISSION_GRANTED
            }
            
            if (deniedPermissions.isNotEmpty()) {
                showPermissionRationale(deniedPermissions)
            }
        }
    }
    
    private fun showPermissionRationale(deniedPermissions: List<String>) {
        val message = buildString {
            append("The following permissions are required:\n\n")
            deniedPermissions.forEach { permission ->
                append("• ${getPermissionName(permission)}\n")
            }
        }
        
        android.app.AlertDialog.Builder(activity)
            .setTitle("Permissions Required")
            .setMessage(message)
            .setPositiveButton("Grant") { dialog, _ ->
                requestAllPermissions()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun getPermissionName(permission: String): String {
        return when (permission) {
            Manifest.permission.INTERNET -> "Internet access (to connect to monitor)"
            Manifest.permission.ACCESS_NETWORK_STATE -> "Network state (to check connection)"
            Manifest.permission.ACCESS_WIFI_STATE -> "WiFi state (to get IP address)"
            Manifest.permission.POST_NOTIFICATIONS -> "Notifications (to show service status)"
            else -> permission.substringAfterLast('.')
        }
    }
}