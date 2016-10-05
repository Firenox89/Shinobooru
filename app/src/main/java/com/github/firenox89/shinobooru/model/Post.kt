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

    //Post Deserializer
    class Deserializer : ResponseDeserializable<Array<Post>> {
        override fun deserialize(content: String) = Gson().fromJson(content, Array<Post>::class.java)
    }

    //Bitmap Deserializer
    class BitmapDeserializer : ResponseDeserializable<Bitmap> {
        override fun deserialize(inputStream: InputStream) = BitmapFactory.decodeStream(inputStream)
    }

    /**
     * Returns a list view detailed tags.
     * Must be call asynchronously
     */
    fun getTagList(): List<Tag> {
        return tags.split(" ").map { Tag(name = it, board = getBoard()) }
    }

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

    fun loadPreview(handler: (Bitmap?) -> Unit): Unit {
        var bitmap = FileManager.previewBitmapFromCache(id)
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

    fun loadSample(handler: (Bitmap?) -> Unit): Unit {
        loadBitmap(url = sample_url, handler = handler)
    }

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

    fun downloadFile() {
        FileManager.downloadFileToStorage(file_url, this)
    }

    fun downloadJpeg() {
        FileManager.downloadFileToStorage(jpeg_url, this)
    }

    fun downloadSample() {
        FileManager.downloadFileToStorage(sample_url, this)
    }

    fun hasFile(): Boolean {
        return file != null
    }

    fun wasViewd(): Boolean {
        return PostLoader.postViewed(id)
    }

    fun getBoard(): String {
        val pattern = Pattern.compile("http[s]?://(?:files\\.)?([a-z\\.]*)")
        val matcher = pattern.matcher(file_url)
        matcher.find()
        return matcher.group(1)
    }
}
