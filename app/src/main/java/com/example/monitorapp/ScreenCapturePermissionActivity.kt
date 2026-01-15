package com.example.monitorapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ScreenCapturePermissionActivity : AppCompatActivity() {
    
    companion object {
        const val SCREEN_CAPTURE_REQUEST = 1001
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_DATA = "EXTRA_DATA"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request screen capture permission immediately
        MediaProjectionService.initialize(this)
        val captureIntent = MediaProjectionService.getScreenCaptureIntent()
        startActivityForResult(captureIntent, SCREEN_CAPTURE_REQUEST)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == SCREEN_CAPTURE_REQUEST) {
            val resultIntent = Intent()
            
            if (resultCode == Activity.RESULT_OK && data != null) {
                resultIntent.putExtra(EXTRA_RESULT_CODE, resultCode)
                resultIntent.putExtra(EXTRA_DATA, data)
                setResult(Activity.RESULT_OK, resultIntent)
            } else {
                setResult(Activity.RESULT_CANCELED, resultIntent)
            }
            
            finish()
        }
    }
}