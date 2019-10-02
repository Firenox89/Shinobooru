package com.github.firenox89.shinobooru.cloud

import com.github.firenox89.shinobooru.repo.model.DownloadedPost

interface CloudSync {
    fun upload(posts: List<DownloadedPost>)
    suspend fun download(): DownloadedPost
    suspend fun fetchData(): Map<String, List<DownloadedPost>>
}