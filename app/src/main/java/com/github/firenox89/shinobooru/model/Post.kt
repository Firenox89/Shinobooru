package com.github.firenox89.shinobooru.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.github.firenox89.shinobooru.settings.SettingsActivity
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.httpGet
import com.google.gson.Gson
import java.io.File
import java.io.InputStream
import java.io.Serializable
import java.util.regex.Pattern

/**
 * Data class to store the meta information of a post.
 * Contains some utility functions.
 */
data class Post(
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
        var file_url: String = "",
        var is_shown_in_index: Boolean = false,
        var preview_url: String = "",
        var preview_width: Int = 0,
        var preview_height: Int = 0,
        var actual_preview_width: Int = 0,
        var actual_preview_height: Int = 0,
        var sample_url: String = "",
        var sample_width: Int = 0,
        var sample_height: Int = 0,
        var sample_file_size: Int = 0,
        var jpeg_url: String = "",
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
        val lastName: String = "",
        val fileSource: String = "",
        val file: File? = null /*FileManager.fileById(id)*/) : Serializable {

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

    /**
     * Returns a list detailed tags.
     * Must not be call from the ui thread since it loads the tag details via the [ApiWrapper].
     *
     * @return a list of [Tag]
     */
    fun getTagList(): List<Tag> {
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
     * If the preview image width is larger then 250px the image will be resized to this value.
     *
     * @param handler will be called after the image was loaded.
     */
    fun loadPreview(handler: (Bitmap?) -> Unit): Unit {
        var bitmap = BitmapFactory.decodeStream(FileManager.previewBitmapFromCache(id))
        if (bitmap == null || SettingsActivity.disableCaching) {
            loadBitmap(preview_url) {
                if (it != null && it.width > 250) {
                    val height = it.height.toDouble() / it.width * 250
                    bitmap = Bitmap.createScaledBitmap(it, 250, height.toInt(), false)
                    handler.invoke(bitmap)
                    FileManager.previewBitmapToCache(id, bitmap)
                } else {
                    handler.invoke(it)
                    FileManager.previewBitmapToCache(id, it)
                }
            }
        } else {
            handler.invoke(bitmap)
        }
    }

   /**
    * Loads the sample image.
    *
    * @param handler will be called after the image was loaded.
    */
    fun loadSample(handler: (Bitmap?) -> Unit): Unit {
        loadBitmap(url = sample_url, handler = handler)
    }

    /**
     * Loads an image from the given url,
     * if the post was downloaded to storage it will be loaded from there.
     * On an error while downloading the method tries again as many times as retries specified.
     *
     * @param url the url to load from
     * @param retries the number of retries in case of errors, default value is 2
     * @param handler will be called after the image was loaded.
     */
    private fun loadBitmap(url: String, retries: Int = 2, handler: (Bitmap?) -> Unit): Unit {
        if (file != null) {
            //TODO: handle OOMs
            handler.invoke(BitmapFactory.decodeStream(file.inputStream()))
        } else {
            url.httpGet().responseObject(BitmapDeserializer()) { req, res, result ->
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
     * Returns if that post was downloaded
     *
     * @return true if the post exists in storage, false otherwise.
     */
    fun hasFile(): Boolean {
        return file != null
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
    fun getBoard(): String {
        val pattern = Pattern.compile("http[s]?://(?:files\\.)?([a-z\\.]*)")
        val matcher = pattern.matcher(file_url)
        matcher.find()
        return matcher.group(1)
    }
}
