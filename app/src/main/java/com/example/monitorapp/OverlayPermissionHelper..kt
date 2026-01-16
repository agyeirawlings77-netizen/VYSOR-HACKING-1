package com.example.monitorapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object OverlayPermissionHelper {
    
    const val OVERLAY_PERMISSION_REQUEST_CODE = 1003
    
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
    
    fun requestOverlayPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(activity)) {
                showPermissionDialog(activity)
            }
        }
    }
    
    private fun showPermissionDialog(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle("Overlay Permission Required")
            .setMessage(
                "To display visual indicators of monitor actions on your screen, " +
                "we need permission to draw over other apps.\n\n" +
                "This allows you to see:\n" +
                "• Monitor's cursor position\n" +
                "• When monitor clicks/taps\n" +
                "• Visual feedback of remote control\n\n" +
                "Would you like to grant this permission?"
            )
            .setPositiveButton("Grant Permission") { dialog, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${activity.packageName}")
                )
                activity.startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
                dialog.dismiss()
            }
            .setNegativeButton("Not Now") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
    
    fun showPermissionDeniedDialog(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle("Permission Denied")
            .setMessage(
                "Without overlay permission, you won't be able to see visual indicators " +
                "of what the monitor is doing on your screen.\n\n" +
                "You can still use the app, but monitor actions will be invisible to you."
            )
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Grant in Settings") { dialog, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${activity.packageName}")
                )
                activity.startActivity(intent)
                dialog.dismiss()
            }
            .show()
    }
    
    fun handlePermissionResult(activity: Activity, hasPermission: Boolean, onGranted: () -> Unit, onDenied: () -> Unit) {
        if (hasPermission) {
            onGranted()
        } else {
            onDenied()
            showPermissionDeniedDialog(activity)
        }
    }
}