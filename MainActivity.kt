package com.example.codegen

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val input = findViewById<EditText>(R.id.inputCommand)
        val button = findViewById<Button>(R.id.btnGenerate)
        val output = findViewById<TextView>(R.id.outputCode)

        button.setOnClickListener {
            val command = input.text.toString().trim().uppercase()
            output.text = generateCode(command)
        }
    }

    private fun generateCode(command: String): String {
        return when (command) {

            "TIME" -> """
                // Get current time
                val time = java.text.SimpleDateFormat(
                    "HH:mm:ss",
                    java.util.Locale.getDefault()
                ).format(java.util.Date())
            """.trimIndent()

            "WATCH" -> """
                // Simple log watcher
                android.util.Log.d("WATCH", "Watching event")
            """.trimIndent()

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
