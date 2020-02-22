package com.github.firenox89.shinobooru.ui.sync

import androidx.lifecycle.ViewModel
import com.github.firenox89.shinobooru.cloud.NextCloudSyncer
import com.github.firenox89.shinobooru.repo.DataSource
import com.github.firenox89.shinobooru.repo.model.CloudPost
import com.github.firenox89.shinobooru.repo.model.DownloadedPost
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import java.lang.Exception

data class SyncState(val postsOnDevice: List<DownloadedPost>, val postsOnCloud: List<CloudPost>, val postsToUpload: List<DownloadedPost>, val postsToDownload: List<CloudPost>)
data class SyncProgress(val postUploaded: Int, val totalPostsToUpload: Int, val postDownloaded: Int, val totalPostsToDownload: Int, val error: Exception?)

class SyncViewModel(val nextCloudSyncer: NextCloudSyncer, val dataSource: DataSource) : ViewModel() {

    suspend fun loadCloudState(): Result<SyncState, Exception> {
        val localPosts = dataSource.getAllDownloadedPosts()
        return nextCloudSyncer.fetchData().map { cloudPosts ->
            val toUpload = localPosts.findNotInCloud(cloudPosts)
            val toDownload = cloudPosts.findNotInLocal(localPosts)
            SyncState(localPosts, cloudPosts, toUpload, toDownload)
        }
    }

    suspend fun sync(postsToUpload: List<DownloadedPost>, postsToDownload: List<CloudPost>): Channel<SyncProgress> {
        val channel = Channel<SyncProgress>()
        GlobalScope.async {
            val uploadResult = nextCloudSyncer.upload(postsToUpload)

            if (uploadResult is Result.Failure) {
                channel.offer(SyncProgress(0, 0, 0, 0, uploadResult.error))
                channel.close()
            } else {
                val downloadResult = nextCloudSyncer.download(postsToDownload)

                if (downloadResult is Result.Failure) {
                    channel.offer(SyncProgress(0, 0, 0, 0, downloadResult.error))
                }

                channel.close()
            }
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
