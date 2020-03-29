package com.github.firenox89.shinobooru.cloud

import android.content.Context
import com.github.firenox89.shinobooru.repo.DataSource
import com.github.firenox89.shinobooru.repo.model.CloudPost
import com.github.firenox89.shinobooru.repo.model.DownloadedPost
import com.github.firenox89.shinobooru.settings.SettingsManager
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import com.github.kittinunf.result.mapError
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.*
import com.owncloud.android.lib.resources.files.model.RemoteFile
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber
import java.io.File
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.util.concurrent.Executors
import kotlin.coroutines.coroutineContext

const val SHINOBOORU_REMOTE_PATH = "/Shinobooru/"

class NextCloudSyncer(private val appContext: Context, private val dataSource: DataSource) : CloudSync, KoinComponent {
    private val settingsManager: SettingsManager by inject()

    private val networkDispatcher = Executors.newFixedThreadPool(8).asCoroutineDispatcher()
    private val syncDir = dataSource.getSyncDir()

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

    override suspend fun upload(posts: List<DownloadedPost>): Result<Channel<UploadProgress>, Exception> =
            getClient().map { client ->
                val progressChannel = Channel<UploadProgress>()
                CoroutineScope(coroutineContext).async {
                    var currentProgress = UploadProgress(posts.size, 0, null)

                    posts.map { post ->
                        async(networkDispatcher) {
                            Timber.d("Upload $post")
                            UploadFileRemoteOperation(
                                    post.file.absolutePath,
                                    SHINOBOORU_REMOTE_PATH + post.file.name,
                                    post.getMIMEType(),
                                    (post.file.lastModified() / 1000).toString()
                            ).execute(client).let { result ->
                                if (!result.isSuccess) {
                                    Timber.e("Uploaded failed $result")
                                    Result.error(result.exception)
                                } else {
                                    Timber.d("Uploaded $result")
                                    Result.success(Unit)
                                }
                            }
                        }
                    }.forEach {
                        it.await().fold({
                            currentProgress = currentProgress.copy(postsUploaded = currentProgress.postsUploaded + 1)
                            progressChannel.offer(currentProgress)
                        }, { exception ->
                            currentProgress = currentProgress.copy(postsUploaded = currentProgress.postsUploaded + 1, error = exception)
                            progressChannel.offer(currentProgress)
                        })
                    }
                    progressChannel.close()
                }
                progressChannel
            }

    override suspend fun download(posts: List<CloudPost>): Result<ReceiveChannel<DownloadProgress>, Exception> =
            getClient().map { client ->
                CoroutineScope(coroutineContext).produce(networkDispatcher) {
                    var currentProgress = DownloadProgress(posts.size, 0, null)
                    posts.map { post ->
                        async {
                            DownloadFileRemoteOperation(
                                    post.remotePath,
                                    syncDir.absolutePath
                            ).execute(client).let { result ->
                                if (!result.isSuccess) {
                                    Timber.e("Download failed $result")
                                    Result.error(result.exception)
                                } else {
                                    Timber.d("Download $result")
                                    val syncFile = File(syncDir.absolutePath + post.remotePath)
                                    val fileDst = dataSource.getFileDestinationFor(post.fileName)
                                    syncFile.renameTo(fileDst)

                                    Timber.d("Download $fileDst")
                                    DownloadedPost.postFromName(fileDst).mapError {
                                        Timber.e("Failed to parse file $fileDst, deleting it.")
                                        fileDst.delete()
                                        it
                                    }
                                }
                            }
                        }
                    }.forEach {
                        Timber.e("Await $it")
                        it.await().fold({
                            Timber.e("Downloaded $it")
                            currentProgress = currentProgress.copy(postsDownloaded = currentProgress.postsDownloaded + 1)
                            offer(currentProgress)
                        }, { exception ->
                            Timber.e("Downloaded $exception")
                            currentProgress = currentProgress.copy(postsDownloaded = currentProgress.postsDownloaded + 1, error = exception)
                            offer(currentProgress)
                        })
                    }
                    dataSource.refreshLocalPosts()
                    close()
                }
            }

    override suspend fun remove(posts: List<DownloadedPost>): Result<Unit, Exception> = withContext(Dispatchers.IO) {
        Result.of<Unit, Exception> {
            getClient().flatMap { client ->
                posts.forEach { post ->
                    Timber.d("Remove $post")
                    RemoveFileRemoteOperation(
                            SHINOBOORU_REMOTE_PATH + "/" + post.file.name
                    ).execute(client).let { result ->
                        if (!result.isSuccess) {
                            Timber.e("Remove failed $result")
                            return@flatMap Result.error(result.exception)
                        } else {
                            Timber.d("Removed $result")
                        }
                    }
                }
                Result.success(Unit)
            }
        }
    }

    override suspend fun fetchData(): Result<List<CloudPost>, Exception> = withContext(Dispatchers.IO) {
        getClient().flatMap { client ->
            val res = ReadFolderRemoteOperation(SHINOBOORU_REMOTE_PATH).execute(client)
            when {
                res.isSuccess -> {
                    val remoteFiles = res.data as ArrayList<RemoteFile>
                    val list = remoteFiles.filter { !it.remotePath.endsWith("/") }.mapNotNull {
                        val createPostResult = CloudPost.fromRemotePath(
                                it.remotePath,
                                it.remotePath.removePrefix(SHINOBOORU_REMOTE_PATH)
                        )
                        if (createPostResult is Result.Success) {
                            createPostResult.get()
                        } else {
                            createPostResult as Result.Failure
                            Timber.e(createPostResult.error, "Failed to load ${it.remotePath}")
                            null
                        }
                    }
                    Result.success(list)
                }
                res.code == RemoteOperationResult.ResultCode.FILE_NOT_FOUND -> {
                    val createRes = CreateFolderRemoteOperation(SHINOBOORU_REMOTE_PATH, true)
                            .execute(client)
                    when {
                        createRes.isSuccess -> {
                            Result.success(emptyList())
                        }
                        createRes.exception != null -> {
                            Result.error(createRes.exception)
                        }
                        else -> {
                            Result.error(IllegalArgumentException("Create root dir failed with unknown code $createRes"))
                        }
                    }
                }
                res.exception != null -> {
                    Result.error(res.exception)
                }
                else -> {
                    Result.error(IllegalArgumentException("Unhandled result code ${res.code}"))
                }
            }
        }
    }
}