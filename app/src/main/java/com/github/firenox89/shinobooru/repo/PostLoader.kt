package com.github.firenox89.shinobooru.repo

import android.graphics.Bitmap
import com.github.firenox89.shinobooru.repo.model.Post
import com.github.firenox89.shinobooru.repo.model.Tag
import kotlinx.coroutines.channels.Channel

interface PostLoader {
    val board: String
    val tags: String

    fun getIndexOf(post: Post): Int
    suspend fun onRefresh(quantity: Int)
    fun getCount(): Int
    suspend fun requestNextPosts(quantity: Int = 20)
    fun getPostAt(index: Int): Post
    fun downloadPost(currentItem: Int)
    suspend fun loadPreview(post: Post): Bitmap
    suspend fun loadSample(post: Post): Bitmap
    suspend fun getTagList(post: Post): List<Tag>
    suspend fun getRangeChangeEventStream(): Channel<Pair<Int, Int>>
}