package com.github.firenox89.shinobooru.ui.sync

import androidx.lifecycle.ViewModel
import com.github.firenox89.shinobooru.cloud.NextCloudSyncer
import com.github.firenox89.shinobooru.repo.DataSource
import com.github.firenox89.shinobooru.repo.model.CloudPost
import com.github.firenox89.shinobooru.repo.model.DownloadedPost
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import timber.log.Timber
import java.lang.Exception
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

data class SyncState(val postsOnDevice: List<DownloadedPost>, val postsOnCloud: List<CloudPost>, val postsToUpload: List<DownloadedPost>, val postsToDownload: List<CloudPost>)
data class SyncProgress(val totalPostsToUpload: Int, val postUploaded: Int, val totalPostsToDownload: Int, val postDownloaded: Int, val error: Exception?)

class SyncViewModel(val nextCloudSyncer: NextCloudSyncer, val dataSource: DataSource) : ViewModel() {

    suspend fun loadCloudState(): Result<SyncState, Exception> {
        val localPosts = dataSource.getAllDownloadedPosts()
        return nextCloudSyncer.fetchData().map { cloudPosts ->
            val toUpload = localPosts.findNotInCloud(cloudPosts)
            val toDownload = cloudPosts.findNotInLocal(localPosts)
            SyncState(localPosts, cloudPosts, toUpload, toDownload)
        }
    }

    fun sync(postsToUpload: List<DownloadedPost>, postsToDownload: List<CloudPost>): Channel<SyncProgress> {
        val channel = Channel<SyncProgress>()
        var currentProgress = SyncProgress(postsToUpload.size, 0, postsToDownload.size, 0, null)
        GlobalScope.async(Dispatchers.IO) {
            nextCloudSyncer.upload(postsToUpload).fold({ uploadStateChannel ->
                for (state in uploadStateChannel) {
                    if (state.error != null) {
                        channel.send(currentProgress.copy(error = state.error))
                        channel.close(CancellationException(state.error?.message))
                        cancel()
                    } else {
                        currentProgress = currentProgress.copy(postUploaded = state.postsUploaded)
                    }
                }
            }, { exception ->
                channel.offer(SyncProgress(0, 0, 0, 0, exception))
                channel.close()
                cancel(CancellationException(exception.message))
            })

            nextCloudSyncer.download(postsToDownload).fold({ downloadStateChannel ->
                Timber.w("dot state $downloadStateChannel")
                for (state in downloadStateChannel) {
                    Timber.w("read download state $state")
                    if (state.error != null) {
                        channel.send(currentProgress.copy(error = state.error))
                        channel.close(CancellationException(state.error?.message))
                        cancel()
                    } else {
                        currentProgress = currentProgress.copy(postDownloaded = state.postsDownloaded)
                    }
                }
            }, { exception ->
                channel.offer(SyncProgress(0, 0, 0, 0, exception))
                channel.close()
                cancel(CancellationException(exception.message))
            })
        }

        return channel
    }
}

private fun List<CloudPost>.findNotInLocal(localPosts: List<DownloadedPost>): List<CloudPost> {
    val notInLocal = mutableListOf<CloudPost>()
    for (cloudPost in this) {
        if (!localPosts.any { it.getBoard() == cloudPost.getBoard() && it.id == cloudPost.id }) {
            notInLocal.add(cloudPost)
        }
    }
    return notInLocal
}

private fun List<DownloadedPost>.findNotInCloud(cloudPosts: List<CloudPost>): List<DownloadedPost> {
    val notInCloud = mutableListOf<DownloadedPost>()
    for (localPost in this) {
        if (!cloudPosts.any { it.getBoard() == localPost.getBoard() && it.id == localPost.id }) {
            notInCloud.add(localPost)
        }
    }
    return notInCloud
}
