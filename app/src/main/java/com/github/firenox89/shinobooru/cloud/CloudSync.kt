package com.github.firenox89.shinobooru.cloud

import com.github.firenox89.shinobooru.repo.model.CloudPost
import com.github.firenox89.shinobooru.repo.model.DownloadedPost
import com.github.kittinunf.result.Result
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import java.lang.Exception

data class DownloadProgress(val totalPostCount: Int, val postsDownloaded: Int, var error: Exception?)
data class UploadProgress(val totalPostCount: Int, val postsUploaded: Int, var error: Exception?)


interface CloudSync {
    suspend fun upload(posts: List<DownloadedPost>): Result<Channel<UploadProgress>, Exception>
    suspend fun download(posts: List<CloudPost>): Result<ReceiveChannel<DownloadProgress>, Exception>
    suspend fun remove(posts: List<DownloadedPost>): Result<Unit, Exception>
    suspend fun fetchData(): Result<List<CloudPost>, Exception>
}