package com.github.firenox89.shinobooru.cloud

import com.github.firenox89.shinobooru.repo.model.DownloadedPost
import io.reactivex.Flowable

interface CloudSync {
    fun upload(posts: List<DownloadedPost>)
    fun download(): Flowable<DownloadedPost>
    fun fetchData(): Flowable<Map<String, List<DownloadedPost>>>
}