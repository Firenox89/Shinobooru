package com.github.firenox89.shinobooru.repo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.github.firenox89.shinobooru.repo.model.Post
import com.github.firenox89.shinobooru.repo.model.Tag
import com.github.firenox89.shinobooru.settings.SettingsActivity
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.coroutines.awaitObject
import com.github.kittinunf.fuel.httpGet
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.InputStream

/**
 * Class in charge of handling post loading.
 * Use [getLoader] to get an instance.
 */
open class RemotePostLoader(override val board: String, override val tags: String) : PostLoader {

    private val initLoadSize = 40
    private val posts = mutableListOf<Post>()
    private val rangeChangeEventStream = Channel<Pair<Int, Int>>()

    private var currentPage = 1

    init {
        GlobalScope.launch {
            requestNextPosts(initLoadSize)
        }
    }

    /**
     * Get a [Post] for a given index.
     *
     * @param index of a post
     * @return [Post] for the given index or null
     */
    override fun getPostAt(index: Int): Post {
        return posts[index]
    }

    override fun downloadPost(index: Int) {
        FileManager.downloadFileToStorage(posts[index].file_url, posts[index])
    }

    /**
     * Not yet implemented.
     */
    fun loadPostFromID() {
//        val wrapper = ApiWrapper("http://$fileSource")
//        wrapper.request(limit = 1) {
//            val currentId = it?.get(0)?.id
//            if (currentId != null) {
//                wrapper.request(limit = 1, page = ((currentId - id).toInt())) {
//                    Log.e("load from id", "expected = $id got = ${it?.get(0)?.id}")
//                }
//            }
//        }
    }

    /**
     * Returns a list of detailed tags.
     * Must not be call from the ui thread since it loads the tag details via the [ApiWrapper].
     *
     * @return a list of [Tag]
     */
    override suspend fun getTagList(post: Post): List<Tag> {
        Timber.i("tags = $tags")
        val tags = post.tags.split(" ").map { Tag(name = it, board = post.getBoard()) }
        return tags
//        emitter.onNext(tags.map { it.loadColor(); it })
    }

    /**
     * Tries to load the preview image from the cache, if not found load it from the board and cache it.
     * If the post was already downloaded a sub sampled version gets loaded.
     *
     * @param handler will be called after the image was loaded.
     */
    override suspend fun loadPreview(post: Post): Bitmap =
            if (!FileManager.isPreviewBitmapCached(post.getBoard(), post.id) || SettingsActivity.disableCaching) {
                loadBitmap(post.preview_url)
                        .also { bitmap ->
                            FileManager.previewBitmapToCache(post.getBoard(), post.id, bitmap)
                        }
            } else {
                BitmapFactory.decodeStream(FileManager.previewBitmapFromCache(post.getBoard(), post.id))
            }

    /**
     * Loads the sample image.
     *
     * @param handler will be called after the image was loaded.
     */
    override suspend fun loadSample(post: Post) = loadBitmap(post.sample_url)

    /**
     * Loads an image from the given url.
     * On an error while downloading the method tries again as many times as retries specified.
     *
     * @param url the url to load from
     * @param retries the number of retries in case of errors, default value is 2
     * @param handler will be called after the image was loaded.
     */
    private suspend fun loadBitmap(url: String) = url.httpGet().awaitObject(BitmapDeserializer())

    /**
     * Class to turn an [InputStream] into a [Bitmap].
     */
    class BitmapDeserializer : ResponseDeserializable<Bitmap> {
        override fun deserialize(inputStream: InputStream) = BitmapFactory.decodeStream(inputStream)
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
    override suspend fun requestNextPosts(quantity: Int) {
        Timber.i("Request $quantity posts from $board")
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
            rangeChangeEventStream.send(Pair(currentSize, count))

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
    override fun getCount(): Int {
        return posts.size
    }

    /**
     * Return the index of a given post in the post list.
     *
     * @param post to get the index of
     * @return index of the given post
     */
    override fun getIndexOf(post: Post): Int {
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
    override suspend fun getRangeChangeEventStream() = rangeChangeEventStream

    /**
     * Will clear the list and reload every post in it.
     * If the given quantity is below 0 the current number of posts will be used.
     *
     * @param quantity of posts to load, default value is -1
     */
    override suspend fun onRefresh(quantity: Int) {
        //TODO: insert new images on top instead of reload everything
        val currentCount = getCount()
        posts.clear()
        currentPage = 1
        requestNextPosts(if (quantity < 0) currentCount else quantity)
    }
}