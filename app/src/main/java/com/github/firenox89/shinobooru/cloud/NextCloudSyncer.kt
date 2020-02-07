package com.github.firenox89.shinobooru.cloud

import android.content.Context
import com.github.firenox89.shinobooru.repo.model.DownloadedPost
import com.github.firenox89.shinobooru.settings.SettingsManager
import com.github.kittinunf.result.Result
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber
import java.lang.Exception
import java.lang.IllegalArgumentException

class NextCloudSyncer(val appContext: Context) : CloudSync, KoinComponent {
    private val settingsManager: SettingsManager by inject()

    private fun getClient(): Result<OwnCloudClient, Exception> =
            settingsManager.nextcloudBaseUri?.let { baseUri ->
                Result.of<OwnCloudClient, Exception> {
                    OwnCloudClientFactory.createOwnCloudClient(baseUri, appContext, true).apply {
                        credentials = OwnCloudCredentialsFactory.newBasicCredentials(
                                settingsManager.nextcloudUser,
                                settingsManager.nextcloudPassword
                        )
                    }
                }
            } ?: Result.error(IllegalArgumentException("Nextcloud Uri not set."))

    override fun upload(posts: List<DownloadedPost>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun download(): DownloadedPost {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun fetchData(): Map<String, List<DownloadedPost>> = withContext(Dispatchers.IO) {
        ReadFolderRemoteOperation("/").execute(getClient().get()).let { result ->
            if (result.isSuccess) {
                val remoteFiles = result.data as ArrayList<RemoteFile>
                remoteFiles.forEach {
                    Timber.w(it.remotePath)
                }
            } else {
                Timber.e(result.exception)
            }
        }

        emptyMap<String, List<DownloadedPost>>()
    }
}