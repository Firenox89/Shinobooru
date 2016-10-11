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
    private val shinobooruImageDir = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES), "shinobooru")

    /** Map of boards and the post images downloaded from them */
    private val boards = mutableMapOf<String, List<Post>>()
    /** List of cached post thumbnails */
    private val cachedFiles = mutableListOf<String>()

    /** Initialize the cached and downloaded post lists. */
    init {
        checkExternalStorage()
        shinobooruImageDir.mkdirs()
        shinobooruImageDir.listFiles().forEach { boards.put(it.name, loadPostListFromFile(it)) }

        // load list of files from cache
        cachedFiles.addAll(Shinobooru.appContext.fileList())
    }

    /**
     * Loading files from a given directory by using [postFromFile] for each file.
     *
     * @param dir the directory to take the files from
     * @return a list of loaded posts
     */
    private fun loadPostListFromFile(dir: File): List<Post> {
        val postList = mutableListOf<Post>()

        dir.listFiles().forEach { postList.add(postFromFile(it)) }
        return postList
    }

    /**
     * Load a post from a file by parsing the file name to get the post id
     */
    private fun postFromFile(postFile: File): Post {
        //TODO: handle non posts
        //compatibility with mbooru saved posts
        val idIndex = if (postFile.name.split(" ")[1].equals("-")) 2 else 1

        val id = postFile.name.split(" ")[idIndex].toLong()
        val source = postFile.name.split(" ")[0].toLowerCase()

        return Post(id = id, fileSource = source, file = postFile)
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

            Log.i("Download", "dest = $shinobooruImageDir/$fileName")

            //next line will be used as destination file
            File(boardSubDir, fileName)
        }.response { request, response, result ->
            //TODO: there have to be a nicer way to kick this off
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
    fun getPosts(board: String): List<Post>? {
        return boards[board]
    }

    /**
     * Returns a list of all downloaded posts by combining the different board lists
     */
    fun getAllPosts(): List<Post> {
        val list = mutableListOf<Post>()
        boards.forEach { list.addAll(it.value) }
        return list
    }

    /**
     * Returns a post from the download lists for the given board name and post id
     *
     * @param board name for the list
     * @param id of the post
     * @return [Post] or null
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
    fun previewBitmapFromCache(id: Long): FileInputStream? {
        if (cachedFiles.contains("$id"))
            return Shinobooru.appContext.openFileInput("$id")
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
    fun previewBitmapToCache(id: Long, bitmap: Bitmap?) {
        val fos = Shinobooru.appContext.openFileOutput("$id", Context.MODE_PRIVATE)
        bitmap?.compress(Bitmap.CompressFormat.PNG, 85, fos)
        fos.close()
        cachedFiles.add("$id")
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