package com.github.firenox89.shinobooru.repo

import android.graphics.Bitmap
import com.github.firenox89.shinobooru.repo.model.Post
import com.github.firenox89.shinobooru.repo.model.Tag
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import kotlinx.coroutines.channels.Channel

interface PostLoader {
    val board: String
    val tags: String

    fun getCount(): Int
    fun getIndexOf(post: Post): Int
    fun getPostAt(index: Int): Post
    suspend fun getRangeChangeEventStream(): Channel<Pair<Int, Int>>
    suspend fun requestNextPosts()
    suspend fun loadPreview(post: Post): Result<Bitmap, Exception>
    suspend fun loadSample(post: Post): Result<Bitmap, Exception>
    suspend fun onRefresh()
}