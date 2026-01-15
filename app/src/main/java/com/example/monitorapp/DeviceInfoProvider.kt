package com.example.monitorapp

import android.content.Context
import android.os.Build
import java.net.Inet4Address
import java.net.NetworkInterface

class DeviceInfoProvider(private val context: Context) {
    
    fun getDeviceModel(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }
    
    fun getAndroidVersion(): String {
        return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }
    
    fun getDeviceName(): String {
        return Build.DEVICE
    }
    
    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress ?: "Unknown"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "Unable to get IP"
    }
    
    fun getDeviceInfo(): Map<String, String> {
        return mapOf(
            "model" to getDeviceModel(),
            "androidVersion" to getAndroidVersion(),
            "deviceName" to getDeviceName(),
            "ipAddress" to getLocalIpAddress()
        )
    }
}