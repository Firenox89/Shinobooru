package com.github.firenox89.shinobooru.cloud

import android.content.Context
import com.github.firenox89.shinobooru.repo.DataSource
import com.github.firenox89.shinobooru.repo.model.CloudPost
import com.github.firenox89.shinobooru.repo.model.DownloadedPost
import com.github.firenox89.shinobooru.settings.SettingsManager
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.*
import com.owncloud.android.lib.resources.files.model.RemoteFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber
import java.lang.Exception
import java.lang.IllegalArgumentException

const val SHINOBOORU_REMOTE_PATH = "/Shinobooru/"

class NextCloudSyncer(private val appContext: Context, private val dataSource: DataSource) : CloudSync, KoinComponent {
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

    override suspend fun upload(posts: List<DownloadedPost>): Result<Unit, Exception> = withContext(Dispatchers.IO) {
        Result.of<Unit, Exception> {
            getClient().flatMap { client ->
                posts.forEach { post ->
                    Timber.d("Upload $post")
                    UploadFileRemoteOperation(
                            post.file.absolutePath,
                            SHINOBOORU_REMOTE_PATH + post.file.name,
                            post.getMIMEType(),
                            (post.file.lastModified() / 1000).toString()
                    ).execute(client).let { result ->
                        if (!result.isSuccess) {
                            Timber.e("Uploaded failed $result")
                            return@flatMap Result.error(result.exception)
                        } else {
                            Timber.d("Uploaded $result")
                        }
                    }
                }
                Result.success(Unit)
            }
        }
    }

    override suspend fun download(posts: List<CloudPost>): Result<List<DownloadedPost>, Exception> = withContext(Dispatchers.IO) {
        Result.of<List<DownloadedPost>, Exception> {
            getClient().map { client ->
                posts.map { post ->
                    val fileDst = dataSource.getFileDestinationFor(post.remotePath.removePrefix(SHINOBOORU_REMOTE_PATH))
                    Timber.d("Download $post to $fileDst")
                    DownloadFileRemoteOperation(
                            post.remotePath,
                            fileDst.absolutePath
                    ).execute(client).let { result ->
                        if (!result.isSuccess) {
                            Timber.e("Download failed $result")
                            Result.error(result.exception)
                        } else {
                            Timber.d("Download $result")
                            DownloadedPost.postFromName(fileDst).map { post ->
                                dataSource.addDownloadedPostToList(post)
                                post
                            }
                        }
                    }
                }
            }.get().map { it.get() }
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
                    val list = remoteFiles.filter { !it.remotePath.endsWith("/") }.map {
                        CloudPost(it.remotePath, it.remotePath.removePrefix(SHINOBOORU_REMOTE_PATH))
                    }
                    Result.success(list)
                }
                res.code == RemoteOperationResult.ResultCode.FILE_NOT_FOUND -> {
                    val createRes = CreateFolderRemoteOperation(SHINOBOORU_REMOTE_PATH, true)
                            .execute(client)
                    if (createRes.isSuccess) {
                        Result.success(emptyList())
                    } else if (createRes.exception != null) {
                        Result.error(createRes.exception)
                    } else {
                        Result.error(IllegalArgumentException("Create root dir failed with unknown code $createRes"))
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