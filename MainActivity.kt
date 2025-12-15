package com.example.timeapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val timeText = findViewById<TextView>(R.id.timeText)
        val calcText = findViewById<TextView>(R.id.calcText)

        val now = Date()
        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeString = format.format(now)

        timeText.text = "Current Time: $timeString"

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        val totalSeconds = hour * 3600 + minute * 60 + second

        calcText.text = """
            Hour: $hour
            Minute: $minute
            Second: $second
            Seconds since midnight: $totalSeconds
        """.trimIndent()
    }
}            """.trimIndent()

            "IP", "IP ADDRESS" -> """
                // Get local IP address
                fun getIpAddress(): String {
                    val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
                    for (intf in interfaces) {
                        val addrs = intf.inetAddresses
                        for (addr in addrs) {
                            if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                                return addr.hostAddress
                            }
                        }
                    }
                    return "Unavailable"
                }
            """.trimIndent()

            else -> "Unknown command"
        }
    }
}
