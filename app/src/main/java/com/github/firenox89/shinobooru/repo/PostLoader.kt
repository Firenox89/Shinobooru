package com.github.firenox89.shinobooru.repo

import android.graphics.Bitmap
import com.github.firenox89.shinobooru.repo.model.Post
import com.github.firenox89.shinobooru.repo.model.Tag
import kotlinx.coroutines.channels.Channel

interface PostLoader {
    val board: String
    val tags: String

    fun downloadPost(currentItem: Int)
    fun getCount(): Int
    fun getIndexOf(post: Post): Int
    fun getPostAt(index: Int): Post
    suspend fun getTagList(post: Post): List<Tag>
    suspend fun getRangeChangeEventStream(): Channel<Pair<Int, Int>>
    suspend fun requestNextPosts()
    suspend fun loadPreview(post: Post): Bitmap
    suspend fun loadSample(post: Post): Bitmap
    suspend fun onRefresh(quantity: Int)
}