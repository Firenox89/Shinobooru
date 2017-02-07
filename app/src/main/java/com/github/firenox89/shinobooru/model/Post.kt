package com.github.firenox89.shinobooru.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.github.firenox89.shinobooru.settings.SettingsActivity
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.httpGet
import com.google.gson.Gson
import org.jetbrains.anko.doAsync
import java.io.File
import java.io.InputStream
import java.io.Serializable
import java.util.concurrent.Executors
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
        val lastName: String = "",
        val file: File? = null /*FileManager.fileById(id)*/) : Serializable {

    private val TAG = "Post"
    private val MAX_FILE_SIZE = 5L * 1024L * 1024L

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
    fun loadPreview(handler: (Bitmap?) -> Unit): Unit {
        doAsync {
            var bitmap = BitmapFactory.decodeStream(FileManager.previewBitmapFromCache(getBoard(), id))
            if (bitmap == null || SettingsActivity.disableCaching) {
                if (file != null) {
                    doAsync {
                        handler.invoke(loadSubsampledImage(file, 250, 400))
                    }
                } else {
                    loadBitmap(preview_url) {
                        handler.invoke(it)
                        FileManager.previewBitmapToCache(getBoard(), id, it)
                    }
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
    fun loadSample(handler: (Bitmap?) -> Unit): Unit {
        if (file != null) {
            doAsync {
                val options = BitmapFactory.Options()
                //sample huge images
                if (file.length() > MAX_FILE_SIZE) {
                    options.inSampleSize = 2
                    options.inDither = true
                    options.inPreferQualityOverSpeed = true
                }
                handler.invoke(BitmapFactory.decodeFile(file.path, options))
            }
        } else {
            loadBitmap(url = sample_url, handler = handler)
        }
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
     * Loads a sub sampled imaae from a given file.
     * The sample rate gets determined by the given width and height and the image size.
     */
    private fun loadSubsampledImage(file: File, reqWidth: Int, reqHeight: Int): Bitmap {
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

        return BitmapFactory.decodeFile(file.path, options)
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

    override fun toString(): String {
        return "Post(id=$id, tags='$tags', created_at=$created_at, creator_id=$creator_id, author='$author', change=$change, source='$source', score=$score, md5='$md5', file_size=$file_size, is_shown_in_index=$is_shown_in_index, preview_width=$preview_width, preview_height=$preview_height, actual_preview_width=$actual_preview_width, actual_preview_height=$actual_preview_height, sample_width=$sample_width, sample_height=$sample_height, sample_file_size=$sample_file_size, jpeg_width=$jpeg_width, jpeg_height=$jpeg_height, jpeg_file_size=$jpeg_file_size, rating='$rating', has_children=$has_children, parent_id=$parent_id, status='$status', width=$width, height=$height, is_held=$is_held, frames_pending_string='$frames_pending_string', frames_string='$frames_string', firstName='$firstName', lastName='$lastName', file=$file, TAG='$TAG')"
    }
}
