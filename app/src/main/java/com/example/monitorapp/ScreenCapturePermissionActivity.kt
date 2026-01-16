package com.example.monitorapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class ScreenCapturePermissionActivity : AppCompatActivity() {
    
    companion object {
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_DATA = "EXTRA_DATA"
    }
    
    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val resultIntent = Intent()
        
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            resultIntent.putExtra(EXTRA_RESULT_CODE, result.resultCode)
            resultIntent.putExtra(EXTRA_DATA, result.data)
            setResult(Activity.RESULT_OK, resultIntent)
        } else {
            setResult(Activity.RESULT_CANCELED, resultIntent)
        }
        
        finish()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            MediaProjectionService.initialize(this)
            val captureIntent = MediaProjectionService.getScreenCaptureIntent()
            screenCaptureLauncher.launch(captureIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
}