package com.github.firenox89.shinobooru.cloud

import com.github.firenox89.shinobooru.repo.model.CloudPost
import com.github.firenox89.shinobooru.repo.model.DownloadedPost
import com.github.kittinunf.result.Result
import java.lang.Exception

interface CloudSync {
    suspend fun upload(posts: List<DownloadedPost>): Result<Unit, Exception>
    suspend fun download(posts: List<CloudPost>): Result<List<DownloadedPost>, Exception>
    suspend fun remove(posts: List<DownloadedPost>): Result<Unit, Exception>
    suspend fun fetchData(): Result<List<CloudPost>, Exception>
}