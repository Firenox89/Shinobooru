package com.github.firenox89.shinobooru.model

import com.github.firenox89.shinobooru.settings.SettingsActivity
import rx.Observable
import rx.lang.kotlin.PublishSubject

object PostLoader {

    private val initLoadSize = 40
    private val posts = mutableListOf<Post>()
    private val rangeChangeEventStream = PublishSubject<Pair<Int, Int>>()
    private var fileMode = false
    private val viewedList = FileManager.loadViewedList() ?: mutableListOf<Long>()

    init {
        requestNextPosts(initLoadSize)
    }

    fun getPostAt(position: Int): Post? {
        return posts[position]
    }

    fun requestNextPosts(quantity: Int = 20) {
        //nothing to request in file mode
        if (!fileMode) {
            ApiWrapper.request {
                //TODO: order results before adding
                val currentSize = posts.size
                val tmpList = mutableListOf<Post>()
                it?.forEach {
                    if (SettingsActivity.filterRating(it.rating)) {
                        tmpList.add(it)
                    }
                }
                val count = tmpList.size
                posts.addAll(tmpList)
                rangeChangeEventStream.onNext(Pair(currentSize, count))

                if (count < quantity)
                    requestNextPosts(quantity - count)
            }
        }
    }

    fun getCount(): Int {
        return posts.size
    }

    fun getPositionFor(post: Post): Int {
        return posts.indexOf(post)
    }

    fun getRangeChangeEventStream(): Observable<Pair<Int, Int>> {
        return rangeChangeEventStream.asObservable()
    }

    fun onRefresh(quantity: Int = -1) {
        //TODO: insert new images on top instead of reload everything
        val currentcount = getCount()
        posts.clear()
        ApiWrapper.onRefresh()
        requestNextPosts(if (quantity < 0) currentcount else quantity)
    }

    fun setBaseURL(url: String) {
        fileMode = false
        ApiWrapper.setBaseURL(url)
        onRefresh(initLoadSize)
    }

    fun getCurrentURL(): String {
        return ApiWrapper.url
    }

    fun setFileMode() {
        fileMode = true
        val postList = FileManager.getPosts()
        if (postList != null) {
            posts.clear()
            posts.addAll(postList)
            rangeChangeEventStream.onNext(Pair(0, postList.size))
        }
    }

    fun addPostIdToViewedList(id: Long) {
        viewedList.add(id)
        //TODO: hook this to app closing event
        FileManager.saveViewedList(viewedList)
    }

    fun postViewed(id: Long): Boolean {
        return id in viewedList
    }
}