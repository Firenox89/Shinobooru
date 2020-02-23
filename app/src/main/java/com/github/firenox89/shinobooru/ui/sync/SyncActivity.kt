package com.github.firenox89.shinobooru.ui.sync

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.ui.base.BaseActivity
import kotlinx.android.synthetic.main.activity_sync.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import timber.log.Timber

class SyncActivity : BaseActivity() {
    private val viewModel: SyncViewModel by inject()
    override fun onCreate(savedInstanceState: Bundle?) {
        //TODO check permissions
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sync)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            viewModel.loadCloudState().fold({ state ->
                postsOnDeviceText.text = state.postsOnDevice.size.toString()
                postsOnCloudText.text = state.postsOnCloud.size.toString()
                postsToUpload.text = state.postsToUpload.size.toString()
                postsToDownload.text = state.postsToDownload.size.toString()

                syncButton.setOnClickListener {
                    //to avoid starting this twice
                    syncButton.isEnabled = false
                    val syncProgress = viewModel.sync(state.postsToUpload, state.postsToDownload)
                    lifecycleScope.launch {
                        for (progress in syncProgress) {
                            if (progress.error != null) {
                                toast("${progress.error.message}")
                                errorText.text = progress.error.message
                            }
                            syncProgressText.text = String.format(
                                    getString(R.string.cloudSyncState),
                                    progress.postUploaded,
                                    progress.totalPostsToUpload,
                                    progress.postDownloaded,
                                    progress.totalPostsToDownload)
                        }

                        toast("Sync complete")
                        syncButton.isEnabled = true
                    }
                }
                syncButton.isEnabled = true
            }, { exception ->
                Timber.e(exception)
                toast("${exception.message}")
            })
        }
    }

    private suspend fun toast(msg: String) = withContext(Dispatchers.Main) {
        Toast.makeText(this@SyncActivity, msg, Toast.LENGTH_LONG).show()
    }
}