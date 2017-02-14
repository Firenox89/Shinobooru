package com.github.firenox89.shinobooru.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.github.firenox89.shinobooru.settings.SettingsActivity
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.httpGet
import com.google.gson.Gson
import org.jetbrains.anko.doAsync
import java.io.InputStream
import java.io.Serializable
import java.util.concurrent.Executors
import java.util.regex.Pattern

/**
 * Data class to store the meta information of a post.
 * Contains some utility functions.
 */
open class Post(
        var id: Long = 0,
        var tags: String = "",
        var created_at: Int = 0,
        var creator_id: Int = 0,
        var author: String = "",
        var change: Int = 0,
        var source: String = "",
        var score: Int = 0,
        var md5: String = "",
        var file_size: Int = 0,
        var is_shown_in_index: Boolean = false,
        var preview_width: Int = 0,
        var preview_height: Int = 0,
        var actual_preview_width: Int = 0,
        var actual_preview_height: Int = 0,
        var sample_width: Int = 0,
        var sample_height: Int = 0,
        var sample_file_size: Int = 0,
        var jpeg_width: Int = 0,
        var jpeg_height: Int = 0,
        var jpeg_file_size: Int = 0,
        var rating: String = "",
        var has_children: Boolean = false,
        var parent_id: Int = 0,
        var status: String = "",
        var width: Int = 0,
        var height: Int = 0,
        var is_held: Boolean = false,
        var frames_pending_string: String = "",
        //        var frames_pending : Array<String>,
        var frames_string: String = "",
        //        var frames : Array<String>,
        val firstName: String = "",
        val lastName: String = "") {

    private val TAG = "Post"

    /**
     * konachan removed the 'http:' part from their links so we have to add it if it is missing.
     */
    var preview_url: String = ""
        get() = "${if (field.startsWith("//")) "http:" else ""}$field"
    var file_url: String = ""
        get() = "${if (field.startsWith("//")) "http:" else ""}$field"
    var sample_url: String = ""
        get() = "${if (field.startsWith("//")) "http:" else ""}$field"
    var jpeg_url: String = ""
        get() = "${if (field.startsWith("//")) "http:" else ""}$field"

    /**
     * Class to turn a JSON array into an array of posts using [Gson].
     */
    class PostDeserializer : ResponseDeserializable<Array<Post>> {
        override fun deserialize(content: String) = Gson().fromJson(content, Array<Post>::class.java)
    }

    /**
     * Class to turn an [InputStream] into a [Bitmap].
     */
    class BitmapDeserializer : ResponseDeserializable<Bitmap> {
        override fun deserialize(inputStream: InputStream) = BitmapFactory.decodeStream(inputStream)
    }

    companion object{
        val boardPattern = Pattern.compile("http[s]?://(?:files\\.)?([a-z\\.]*)")
    }

    /**
     * Returns a list of detailed tags.
     * Must not be call from the ui thread since it loads the tag details via the [ApiWrapper].
     *
     * @return a list of [Tag]
     */
    fun getTagList(): List<Tag> {
        Log.i(TAG, "tags = $tags")
        return tags.split(" ").map { Tag(name = it, board = getBoard()) }
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
     * Tries to load the preview image from the cache, if not found load it from the board and cache it.
     * If the post was already downloaded a sub sampled version gets loaded.
     *
     * @param handler will be called after the image was loaded.
     */
    open fun loadPreview(handler: (Bitmap?) -> Unit): Unit {
        doAsync(Throwable::printStackTrace) {
            var bitmap = BitmapFactory.decodeStream(FileManager.previewBitmapFromCache(getBoard(), id))
            if (bitmap == null || SettingsActivity.disableCaching) {
                loadBitmap(preview_url) {
                    handler.invoke(it)
                    FileManager.previewBitmapToCache(getBoard(), id, it)
                }
            } else {
                handler.invoke(bitmap)
            }
        }
    }

    /**
     * Loads the sample image.
     *
     * @param handler will be called after the image was loaded.
     */
    open fun loadSample(handler: (Bitmap?) -> Unit): Unit {
        loadBitmap(url = sample_url, handler = handler)
    }

    /**
     * Loads an image from the given url.
     * On an error while downloading the method tries again as many times as retries specified.
     *
     * @param url the url to load from
     * @param retries the number of retries in case of errors, default value is 2
     * @param handler will be called after the image was loaded.
     */
    private fun loadBitmap(url: String, retries: Int = 2, handler: (Bitmap?) -> Unit): Unit {
        url.httpGet().apply { callbackExecutor = Executors.newSingleThreadExecutor() }
                .responseObject(BitmapDeserializer()) { req, res, result ->
                    val (bitmap, err) = result
                    if (err != null) {
                        Log.e("Http request error", "$err")
                        if (retries > 0) {
                            //TODO: improve that
                            //try again
                            Thread.sleep(500)
                            loadBitmap(url, retries - 1, handler)
                        }
                    } else {
                        handler(bitmap)
                    }
                }
    }

    /**
     * Downloads the original file to the storage using [FileManager.downloadFileToStorage]
     */
    fun downloadFile() {
        FileManager.downloadFileToStorage(file_url, this)
    }

    /**
     * Downloads the jpeg image to the storage using [FileManager.downloadFileToStorage]
     */
    fun downloadJpeg() {
        FileManager.downloadFileToStorage(jpeg_url, this)
    }

    /**
     * Downloads the sample image to the storage using [FileManager.downloadFileToStorage]
     */
    fun downloadSample() {
        FileManager.downloadFileToStorage(sample_url, this)
    }

    /**
     * Returns if that post was viewed.
     *
     * @return if viewed true, false otherwise
     */
    fun wasViewed(): Boolean {
        return PostLoader.postViewed(id)
    }

    /**
     * Parse and return the board name from the file url.
     *
     * @return the board name this post belongs to.
     */
    open fun getBoard(): String {
        val matcher = boardPattern.matcher(file_url)
        matcher.find()
        return matcher.group(1)
    }

    override fun toString(): String {
        return "Post(id=$id, tags='$tags', created_at=$created_at, creator_id=$creator_id," +
                " author='$author', change=$change, source='$source', score=$score, md5='$md5'," +
                " file_size=$file_size, is_shown_in_index=$is_shown_in_index," +
                " preview_width=$preview_width, preview_height=$preview_height," +
                " actual_preview_width=$actual_preview_width," +
                " actual_preview_height=$actual_preview_height, sample_width=$sample_width," +
                " sample_height=$sample_height, sample_file_size=$sample_file_size," +
                " jpeg_width=$jpeg_width, jpeg_height=$jpeg_height, jpeg_file_size=$jpeg_file_size," +
                " rating='$rating', has_children=$has_children, parent_id=$parent_id," +
                " status='$status', width=$width, height=$height, is_held=$is_held," +
                " frames_pending_string='$frames_pending_string', frames_string='$frames_string'," +
                " firstName='$firstName', lastName='$lastName', TAG='$TAG')"
    }
}
