package com.example.codegenerator

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var templates: Map<String, String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val input = findViewById<EditText>(R.id.commandInput)
        val button = findViewById<Button>(R.id.generateBtn)
        val output = findViewById<TextView>(R.id.codeOutput)

        // REGISTER ALL CODE TEMPLATES HERE
        templates = mapOf(
            "TIME" to timeTemplate(),
            "IP" to ipTemplate(),
            "DATE" to dateTemplate(),
            "COUNTER" to counterTemplate(),
            "CALCULATOR" to calculatorTemplate()
        )

        button.setOnClickListener {
            val key = input.text.toString().trim().uppercase()
            output.text = templates[key] ?: "NO TEMPLATE FOUND FOR: $key"
        }
    }

    // ================= TEMPLATES =================

    private fun timeTemplate(): String = """
        ===== MainActivity.kt =====
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

                val tv = findViewById<TextView>(R.id.text)
                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                tv.text = "Current Time: $time"
            }
        }

        ===== activity_main.xml =====
        <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:gravity="center">

            <TextView
                android:id="@+id/text"
                android:textSize="20sp"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"/>
        </LinearLayout>
    """.trimIndent()

    private fun ipTemplate(): String = """
        ===== MainActivity.kt =====
        package com.example.ipapp

        import android.os.Bundle
        import androidx.appcompat.app.AppCompatActivity
        import android.widget.TextView
        import kotlin.random.Random

        class MainActivity : AppCompatActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.activity_main)

                val tv = findViewById<TextView>(R.id.text)
                tv.text = "IP: ${'$'}{Random.nextInt(1,255)}.${'$'}{Random.nextInt(1,255)}.${'$'}{Random.nextInt(1,255)}.${'$'}{Random.nextInt(1,255)}"
            }
        }

        ===== activity_main.xml =====
        <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:gravity="center">

            <TextView
                android:id="@+id/text"
                android:textSize="20sp"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"/>
        </LinearLayout>
    """.trimIndent()

    private fun dateTemplate(): String = """
        ===== MainActivity.kt =====
        package com.example.dateapp

        import android.os.Bundle
        import androidx.appcompat.app.AppCompatActivity
        import android.widget.TextView
        import java.util.*

        class MainActivity : AppCompatActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.activity_main)

                val tv = findViewById<TextView>(R.id.text)
                tv.text = Date().toString()
            }
        }
    """.trimIndent()

    private fun counterTemplate(): String = """
        ===== MainActivity.kt =====
        package com.example.counterapp

        import android.os.Bundle
        import androidx.appcompat.app.AppCompatActivity
        import android.widget.*

        class MainActivity : AppCompatActivity() {
            var count = 0
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.activity_main)

                val tv = findViewById<TextView>(R.id.text)
                val btn = findViewById<Button>(R.id.btn)

                btn.setOnClickListener {
                    count++
                    tv.text = count.toString()
                }
            }
        }
    """.trimIndent()

    private fun calculatorTemplate(): String = """
        ===== MainActivity.kt =====
        package com.example.calcapp

        import android.os.Bundle
        import androidx.appcompat.app.AppCompatActivity
        import android.widget.*

        class MainActivity : AppCompatActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.activity_main)

                val a = findViewById<EditText>(R.id.a)
                val b = findViewById<EditText>(R.id.b)
                val r = findViewById<TextView>(R.id.result)

                findViewById<Button>(R.id.btn).setOnClickListener {
                    r.text = (a.text.toString().toInt() + b.text.toString().toInt()).toString()
                }
            }
        }
    """.trimIndent()
}
