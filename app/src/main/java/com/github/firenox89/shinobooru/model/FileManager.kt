package com.github.firenox89.shinobooru.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import com.github.firenox89.shinobooru.app.Shinobooru
import com.github.kittinunf.fuel.httpDownload
import java.io.*
import java.util.regex.Pattern

/**
 * In charge of handling file system tasks.
 */
object FileManager {

    /** The image directory inside androids picture dir. */
    val shinobooruImageDir = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES), "shinobooru")

    /** Map of boards and the post images downloaded from them */
    val boards = mutableMapOf<String, List<DownloadedPost>>()
    /** List of cached post thumbnails */
    private val cachedFiles = mutableListOf<String>()

    //TODO into the settings with you
    val maxCachedSize = 1000

    val TAG = "FileManager"

    /** Initialize the cached and downloaded post lists. */
    init {
        checkExternalStorage()
        shinobooruImageDir.mkdirs()
        shinobooruImageDir.listFiles().forEach { boards.put(it.name, loadDownloadedPostListFromFile(it)) }

        // load list of files from cache

        cachedFiles.addAll(Shinobooru.appContext.fileList())
    }

    /**
     * Loading files from a given directory by using [postFromName] for each file.
     *
     * @param dir the directory to take the files from
     * @return a list of loaded posts
     */
    private fun loadDownloadedPostListFromFile(dir: File): List<DownloadedPost> {
        val postList = mutableListOf<DownloadedPost>()

        dir.listFiles().forEach { postList.add(postFromName(it.name, it)) }
        return postList
    }

    /**
     * Load a post from a file by parsing the file name to get the post id
     */
    fun postFromName(postFileName: String, postFile: File): DownloadedPost {
        //TODO: handle non posts
        //compatibility with mbooru saved posts
        val idIndex = if (postFileName.split(" ")[1].equals("-")) 2 else 1

        val id = postFileName.split(" ")[idIndex].toLong()
        val source = postFileName.split(" ")[0].toLowerCase()

        return DownloadedPost(id = id, file = postFile)
    }

    /**
     * Asynchronously downloads a given post from a given url into [shinobooruImageDir].
     * The name of the file will be composed like:
     * '<board>/<board> <postId> <tags>.<dataType>'
     *
     * @param url to load from
     * @param post to get the id and tags from
     */
    fun downloadFileToStorage(url: String, post: Post) {
        checkExternalStorage()
        val pattern = Pattern.compile("http[s]?://(?:files\\.)?([a-z\\.]*)")
        val matcher = pattern.matcher(url)
        matcher.find()
        val board = matcher.group(1)

        url.httpDownload().destination { res, realUrl ->
            val boardSubDirName = board
            val dataType = url.split(".").last()
            val fileName = "$board ${post.id} ${post.tags}.$dataType"
            val boardSubDir = File(shinobooruImageDir, boardSubDirName)

            boardSubDir.mkdirs()

            Log.i("Download", "dest = $boardSubDir/$fileName")

            //next line will be used as destination file
            File(boardSubDir, fileName)
        }.response { request, response, result ->
            if (result.component2() != null)
                Log.d("Download", "Error = ${result.component2()}")
        }
    }

    /** Check for Storage availability */
    fun checkExternalStorage() {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            throw IOException("External storage not mounted")
        }
    }

    /**
     * Returns a list of downloaded posts for a given board name
     *
     * @param board name to get the list from
     * @return list of posts for the given board
     */
    fun getDownloadedPosts(board: String): List<DownloadedPost>? {
        return boards[board]
    }

    /**
     * Returns a list of all downloaded posts by combining the different board lists
     */
    fun getAllDownloadedPosts(): List<DownloadedPost> {
        val list = mutableListOf<DownloadedPost>()
        boards.forEach { list.addAll(it.value) }
        return list
    }

    /**
     * Returns a post from the download lists for the given board name and post id
     *
     * @param board name for the list
     * @param id of the post
     * @return [DownloadedPost] or null
     */
    fun fileById(board: String, id: Long): File? {
        return boards[board]?.filter { it.id == id }?.first()?.file
    }

    /**
     * Return a [FileInputStream] of the cached preview image or null if not cached.
     *
     * @param id of the post the preview belongs to
     * @return a [FileInputStream] or null
     */
    fun previewBitmapFromCache(board: String, id: Long): FileInputStream? {
        if (cachedFiles.contains("$board $id.jpeg"))
            return Shinobooru.appContext.openFileInput("$board $id.jpeg")
        else
            return null
    }

    /**
     * Stores a given [Bitmap] with the given id as name
     * CompressFormat is png
     *
     * @param id for the file name
     * @param bitmap to store
     */
    fun previewBitmapToCache(board: String, id: Long, bitmap: Bitmap?) {
        val fos = Shinobooru.appContext.openFileOutput("$board $id.jpeg", Context.MODE_PRIVATE)
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 85, fos)
        fos.close()
        cachedFiles.add("$id")
        checkCacheSize()
    }

    @Volatile
    var checkingCache = false

    fun checkCacheSize() {
        if (checkingCache)
            return
        if (cachedFiles.size > maxCachedSize) {
            checkingCache = true
            Log.i(TAG, "clean up cache")
            var list = Shinobooru.appContext.filesDir.list().filter { it.endsWith(".jpeg") }
            list.sortedBy { File(it).lastModified() }
            list.drop(500).forEach { File(Shinobooru.appContext.filesDir, it).delete() }

            cachedFiles.clear()
            cachedFiles.addAll(Shinobooru.appContext.fileList())
            checkingCache = false
        }
    }

    /**
     * Save the given list of id from viewed posts to the storage.
     *
     * @param viewedList to save
     */
    fun saveViewedList(viewedList: MutableList<Long>) {
        val outputStream = Shinobooru.appContext.openFileOutput("ViewList", Context.MODE_PRIVATE)
        ObjectOutputStream(outputStream).apply { writeObject(viewedList) }.close()
    }

    /**
     * Load the list of viewed posts from the storage.
     *
     * @return the list or null if no list was stored yet
     */
    fun loadViewedList(): MutableList<Long>? {
        if (Shinobooru.appContext.fileList().filter { it.equals("ViewList") }.isNotEmpty()) {
            val inputStream = Shinobooru.appContext.openFileInput("ViewList")
            var list: MutableList<Long>? = null
            ObjectInputStream(inputStream).apply { list = readObject() as MutableList<Long> }.close()
            return list
        }
        return null
    }
}