package com.github.firenox89.shinobooru.utility

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import com.github.firenox89.shinobooru.app.Shinobooru
import com.github.firenox89.shinobooru.model.DownloadedPost
import com.github.firenox89.shinobooru.model.Post
import com.github.firenox89.shinobooru.model.Tag
import com.github.firenox89.shinobooru.settings.SettingsActivity
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.rx.rx_object
import com.google.gson.Gson
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.io.File
import java.io.InputStream
import android.view.Display
import android.content.Context.WINDOW_SERVICE
import android.support.v4.content.ContextCompat.getSystemService
import android.view.WindowManager


/**
 * Class in charge of handling post loading.
 * Use [getLoader] to get an instance.
 */
open class PostLoader {

    companion object {
        private val loaderList = mutableListOf<PostLoader>().apply { add(FileLoader()) }
        private val viewedList = FileManager.loadViewedList() ?: mutableListOf()

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
    private val rangeChangeEventStream = PublishSubject.create<Pair<Int, Int>>()

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
    open fun getPostAt(index: Int): Post {
        return posts[index]
    }

    fun downloadPost(index: Int) {
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
    fun getTagList(post: Post): Flowable<List<Tag>> =
            Flowable.create({ emitter ->
                                Timber.i("tags = $tags")
                                val tags = post.tags.split(" ").map { Tag(name = it, board = post.getBoard()) }
                                emitter.onNext(tags)
                                emitter.onNext(tags.map { it.loadColor(); it })
                            }, BackpressureStrategy.DROP)

    /**
     * Tries to load the preview image from the cache, if not found load it from the board and cache it.
     * If the post was already downloaded a sub sampled version gets loaded.
     *
     * @param handler will be called after the image was loaded.
     */
    fun loadPreview(post: Post): Single<Bitmap> =
            if (post is DownloadedPost) {
                loadPreview(post)
            } else if (!FileManager.isPreviewBitmapCached(post.getBoard(), post.id) || SettingsActivity.disableCaching) {
                loadBitmap(post.preview_url)
                        .map { bitmap ->
                            FileManager.previewBitmapToCache(post.getBoard(), post.id, bitmap)
                            bitmap
                        }
            } else {
                Single.just(BitmapFactory.decodeStream(FileManager.previewBitmapFromCache(post.getBoard(), post.id)))
            }

    /**
     * Loads the sample image.
     *
     * @param handler will be called after the image was loaded.
     */
    fun loadSample(post: Post) =
            if (post is DownloadedPost) {
                loadSample(post)
            } else {
                loadBitmap(post.sample_url)
            }

    /**
     * Loads an image from the given url.
     * On an error while downloading the method tries again as many times as retries specified.
     *
     * @param url the url to load from
     * @param retries the number of retries in case of errors, default value is 2
     * @param handler will be called after the image was loaded.
     */
    private fun loadBitmap(url: String) = url.httpGet().rx_object(BitmapDeserializer()).map { it.get() }


    fun loadSample(downloadedPost: DownloadedPost): Single<Bitmap> {
        val wm = Shinobooru.appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()
        display.getSize(size)
        //sample huge images=
        return loadSubsampledImage(downloadedPost.file, size.x, size.y)
    }

    fun loadPreview(downloadedPost: DownloadedPost): Single<Bitmap> = loadSubsampledImage(downloadedPost.file, 250, 400)
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
        return rangeChangeEventStream.hide()
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

    /**
     * Loads a sub sampled imaae from a given file.
     * The sample rate gets determined by the given width and height and the image size.
     */
    private fun loadSubsampledImage(file: File, reqWidth: Int, reqHeight: Int): Single<Bitmap> =
            Single.create { emitter ->
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(file.path, options)
                // Raw height and width of image
                val height = options.outHeight
                val width = options.outWidth
                var inSampleSize = 1

                if (height > reqWidth || width > reqHeight) {

                    val halfHeight = height / 2
                    val halfWidth = width / 2

                    // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                    // height and width larger than the requested height and width.
                    while (halfHeight / inSampleSize >= reqWidth && halfWidth / inSampleSize >= reqHeight) {
                        inSampleSize *= 2
                    }
                }
                options.inJustDecodeBounds = false
                options.inSampleSize = inSampleSize
                options.inDither = true
                options.inPreferQualityOverSpeed = true

                emitter.onSuccess(BitmapFactory.decodeFile(file.path, options))
            }


}