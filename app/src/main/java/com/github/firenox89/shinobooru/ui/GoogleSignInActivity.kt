package com.github.firenox89.shinobooru.ui

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.cloud.GoogleDrive
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.drive.Drive
import timber.log.Timber

class GoogleSignInActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {
    lateinit var status: TextView
    private var mGoogleApiClient: GoogleApiClient? = null
    private val REQUEST_CODE_SIGN_IN = 0
    val RC_RESOLUTION: Int = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_sign_in)
//        verticalLayout {
//            gravity = Gravity.CENTER
//            status = textView("Connect to Google Drive")
//        }.applyRecursively { view ->
//            when (view) {
//                is TextView -> {
//                    view.padding = dip(10)
//                    view.gravity = Gravity.CENTER
//                    view.textSize = Constants.FONT_SIZE
//                }
//                is LinearLayout -> {
//                    view.gravity = Gravity.CENTER
//                }
//            }
//        }

        val mGoogleSignInClient = buildGoogleSignInClient()
        startActivityForResult(mGoogleSignInClient.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            handleSignInResult(result)
        } else if (requestCode == RC_RESOLUTION) {
            Timber.e("RES RES")
        }
    }

    private fun handleSignInResult(result: GoogleSignInResult) {
        Timber.d("handleSignInResult:" + result.isSuccess)
        Timber.e("status ${result.status}")
        Timber.e("status ${result.status.statusMessage}")
        Timber.e("status ${result.status.statusCode}")
        if (result.isSuccess) {
            Timber.e("Timber.n successful")
            GoogleDrive.googleApiClient = mGoogleApiClient
            runOnUiThread {
                startSyncActivity()
                finish()
            }
        } else if (result.status.hasResolution()){
            Timber.e("has res")
            result.status.startResolutionForResult(this, RC_RESOLUTION)
        }
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        Timber.e("status ${result.errorMessage}")
        Timber.e("status res ${result.hasResolution()}")
        Timber.e("status ${result}")
        runOnUiThread {
            status.text = result.toString()
        }
    }

    fun buildGoogleSignInClient(): GoogleSignInClient {
        val signInOptions =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_FILE, Drive.SCOPE_APPFOLDER)
                        .build()
        return GoogleSignIn.getClient(this, signInOptions)
    }


    fun startSyncActivity() {
        val intent = Intent(this, SyncActivity::class.java)
        startActivity(intent)
    }
}
