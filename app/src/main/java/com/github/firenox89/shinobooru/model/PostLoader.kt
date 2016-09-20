package com.github.firenox89.shinobooru.model

import com.github.firenox89.shinobooru.settings.SettingsActivity
import rx.Observable
import rx.lang.kotlin.PublishSubject

open class PostLoader(val board: String, val tags: String = "") {

    companion object {
        val loaderList = mutableListOf<PostLoader>().apply { add(FileLoader()) }

        fun getLoader(board: String, tags: String = ""): PostLoader {
            var loader = loaderList.find { it.board.equals(board) && it.tags.equals(tags) }
            if (loader == null) {
                loader = PostLoader(board, tags)
                loaderList.add(loader)
            }
            return loader
        }
        private val viewedList = FileManager.loadViewedList() ?: mutableListOf<Long>()

        fun addPostIdToViewedList(id: Long) {
            viewedList.add(id)
            //TODO: hook this to app closing event
            FileManager.saveViewedList(viewedList)
        }

        fun postViewed(id: Long): Boolean {
            return id in viewedList
        }

        fun ratingChanged() {
            loaderList.forEach { it.onRefresh() }
        }
    }

    private val initLoadSize = 40
    private val posts = mutableListOf<Post>()
    private val rangeChangeEventStream = PublishSubject<Pair<Int, Int>>()

    private var currentPage = 1

    init {
        requestNextPosts(initLoadSize)
    }

    open fun getPostAt(position: Int): Post? {
        return posts[position]
    }

    open fun requestNextPosts(quantity: Int = 20) {
            ApiWrapper.request(board, currentPage++, tags) {
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

    open fun getCount(): Int {
        return posts.size
    }

    open fun getPositionFor(post: Post): Int {
        return posts.indexOf(post)
    }

    fun getRangeChangeEventStream(): Observable<Pair<Int, Int>> {
        return rangeChangeEventStream.asObservable()
    }

    open fun onRefresh(quantity: Int = -1) {
        //TODO: insert new images on top instead of reload everything
        val currentcount = getCount()
        posts.clear()
        currentPage = 1
        requestNextPosts(if (quantity < 0) currentcount else quantity)
    }
}