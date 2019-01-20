package com.github.firenox89.shinobooru.repo.model

import android.graphics.Bitmap
import io.reactivex.Flowable
import io.reactivex.Single

interface PostLoader {
    val board: String
    val tags: String

    fun getIndexOf(post: Post): Int
    fun onRefresh(quantity: Int)
    fun getCount(): Int
    fun requestNextPosts(quantity: Int = 20)
    fun getPostAt(index: Int): Post
    fun downloadPost(currentItem: Int)
    fun loadPreview(post: Post): Single<Bitmap>
    fun loadSample(post: Post): Single<Bitmap>
    fun getTagList(post: Post): Single<List<Tag>>
    fun getRangeChangeEventStream(): Flowable<Nothing>
}