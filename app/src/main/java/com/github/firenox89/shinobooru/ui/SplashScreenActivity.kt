package com.github.firenox89.shinobooru.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import com.github.firenox89.shinobooru.R
import android.widget.TextView
import com.github.firenox89.shinobooru.ui.thumbnail.ThumbnailActivity


/**
 * Created by firenox on 2/12/17.
 */
class SplashScreenActivity : Activity() {
    val TAG = "SplashScreenActivity"
    var loadingText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashscreen)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkPermissions()) {
                //no need to initialize without permissions
                return
            }
        }
        startThumbnailActivity()
        finish()
    }

    fun startThumbnailActivity() {
        val intent = Intent(this, ThumbnailActivity::class.java)
        startActivity(intent)
    }

    @SuppressLint("NewApi")
    fun checkPermissions(): Boolean {
        val storage = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (checkSelfPermission(storage) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(storage), 1)
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var allGranted = true
        grantResults?.forEach {
            if (it == PackageManager.PERMISSION_DENIED) {
                allGranted = false
                Log.e(TAG, "File Permission denied")
            }
        }
        //TODO show something when permission was denied
        if (allGranted) {
            Log.d(TAG, "File Permission granted")
            startThumbnailActivity()
            finish()
        } else {
            loadingText?.text = "Permission to access storage was not granted."
        }
    }
}