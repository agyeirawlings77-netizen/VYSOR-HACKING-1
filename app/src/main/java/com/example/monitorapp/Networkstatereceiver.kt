package com.example.monitorapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

class NetworkStateReceiver : BroadcastReceiver() {
    
    interface NetworkCallback {
        fun onNetworkAvailable()
        fun onNetworkLost()
    }
    
    private var callback: NetworkCallback? = null
    
    fun setCallback(callback: NetworkCallback) {
        this.callback = callback
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            if (isNetworkAvailable(it)) {
                callback?.onNetworkAvailable()
            } else {
                callback?.onNetworkLost()
            }
        }
    }
    
    companion object {
        fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                @Suppress("DEPRECATION")
                networkInfo?.isConnected == true
            }
        }
    }
}