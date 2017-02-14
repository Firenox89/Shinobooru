package com.github.firenox89.shinobooru.model

import android.util.Log
import com.github.firenox89.shinobooru.settings.SettingsActivity
import rx.Observable
import rx.lang.kotlin.PublishSubject

/**
 * Class in charge of handling post loading.
 * Use [getLoader] to get an instance.
 */
open class PostLoader {

    companion object {
        val TAG = "PostLoader"
        private val loaderList = mutableListOf<PostLoader>().apply { add(FileLoader()) }
        private val viewedList = FileManager.loadViewedList() ?: mutableListOf<Long>()

        /**
         * Returns a [PostLoader] instance for the given arguments.
         * Cache create instances and return them on the same arguments.
         * To obtain an instance of the [FileLoader] use 'FileLoader' as board name
         *
         * @param board that this loader should load from
         * @param tags this loader should add for requests
         * @return a cached or newly created instance of a [PostLoader]
         */
        fun getLoader(board: String, tags: String = ""): PostLoader {
            var loader = loaderList.find { it.board.equals(board) && it.tags.equals(tags) }
            if (loader == null) {
                loader = PostLoader(board, tags)
                loaderList.add(loader)
            }
            return loader
        }

        //TODO turn list into weakhashmap instead of discarding by hand
        fun discardLoader(loader: PostLoader) {
            if (!loader.board.equals("FileLoader"))
                loaderList.remove(loader)
        }

        /**
         * Add a post id to the list of viewed posts
         *
         * @param id to add
         */
        fun addPostIdToViewedList(id: Long) {
            //TODO implement run-length encoding
            viewedList.add(id)
            //TODO: hook this to app closing event
            FileManager.saveViewedList(viewedList)
        }

        /**
         * Returns true if post id was viewed.
         *
         * @param id the post id
         * @return true if post was viewed, false otherwise
         */
        fun postViewed(id: Long): Boolean {
            return id in viewedList
        }

        /**
         * Reloads the posts for all stored loader instances
         */
        fun ratingChanged() {
            //TODO: set a flag for currently not used loader instead of reloading them all
            loaderList.forEach { it.onRefresh() }
        }
    }

    val board: String
    val tags: String
    private val initLoadSize = 40
    private val posts = mutableListOf<Post>()
    private val rangeChangeEventStream = PublishSubject<Pair<Int, Int>>()

    private var currentPage = 1

    /**
     * Protected so that only [getLoader] and [FileLoader] can create an instance.
     */
    protected constructor(board: String, tags: String = "") {
        this.board = board
        this.tags = tags

        //fill the post list
        requestNextPosts(initLoadSize)
    }

    /**
     * Get a [Post] for a given index.
     *
     * @param index of a post
     * @return [Post] for the given index or null
     */
    open fun getPostAt(index: Int): Post? {
        return posts[index]
    }

    /**
     * Requests new posts from the [ApiWrapper].
     * Results will be filtered according to the current rating settings,
     * The method will continue to request new posts until the given quantity was reached,
     * after every iteration the loaded post will get added to the post list
     * and a rangeChanged event will be fired.
     *
     * @param quantity of post that should be loaded
     */
    open fun requestNextPosts(quantity: Int = 20) {
        ApiWrapper.request(board, currentPage++, tags) {
            //TODO: order results before adding
            val currentSize = posts.size
            val tmpList = mutableListOf<Post>()
            it.forEach {
                //TODO forbid to search with all ratings disabled
                if (SettingsActivity.filterRating(it.rating)) {
                    tmpList.add(it)
                }
            }
            val count = tmpList.size
            posts.addAll(tmpList)
            rangeChangeEventStream.onNext(Pair(currentSize, count))

            // an empty result means that all posts are loaded
            if (count < quantity && it.isNotEmpty()) {
                requestNextPosts(quantity - count)
            }
        }
    }

    /**
     * Return the size of the post list.
     *
     * @return the size of the post list
     */
    open fun getCount(): Int {
        return posts.size
    }

    /**
     * Return the index of a given post in the post list.
     *
     * @param post to get the index of
     * @return index of the given post
     */
    open fun getIndexOf(post: Post): Int {
        return posts.indexOf(post)
    }

    /**
     * Returns a observable of the [rangeChangeEventStream],
     * every time new posts got added to the list
     * an event will be fired containing the previous number of post
     * and the number of added ones.
     *
     * @return the [rangeChangeEventStream]
     */
    fun getRangeChangeEventStream(): Observable<Pair<Int, Int>> {
        return rangeChangeEventStream.asObservable()
    }

    /**
     * Will clear the list and reload every post in it.
     * If the given quantity is below 0 the current number of posts will be used.
     *
     * @param quantity of posts to load, default value is -1
     */
    open fun onRefresh(quantity: Int = -1) {
        //TODO: insert new images on top instead of reload everything
        val currentCount = getCount()
        posts.clear()
        currentPage = 1
        requestNextPosts(if (quantity < 0) currentCount else quantity)
    }
}