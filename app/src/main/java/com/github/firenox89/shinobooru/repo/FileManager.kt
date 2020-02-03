package com.github.firenox89.shinobooru.repo

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import com.github.firenox89.shinobooru.app.Shinobooru
import com.github.firenox89.shinobooru.repo.model.DownloadedPost
import com.github.firenox89.shinobooru.repo.model.Post
import com.github.kittinunf.fuel.httpDownload
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.regex.Pattern

/**
 * In charge of handling file system tasks.
 */
class FileManager(val appContext: Context) {

    /** The image directory inside androids picture dir. */
    val shinobooruImageDir = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES), "shinobooru")

    /** Map of boards and the post images downloaded from them */
    //TODO map and list should be immutable from outside this class
    val boards = mutableMapOf<String, MutableList<DownloadedPost>>()
    /** List of cached post thumbnails */
    private val cachedFiles = mutableListOf<String>()

    //TODO into the settings with you
    private val maxCachedSize = 1000

    @Volatile private var checkingCache = false

    /** Initialize the cached and downloaded post lists. */
    init {
        checkExternalStorage()
        shinobooruImageDir.mkdirs()
        shinobooruImageDir.listFiles().forEach { boards.put(it.name, loadDownloadedPostListFromFile(it)) }

        // load list of files from cache
        cachedFiles.addAll(appContext.fileList())
    }

    /**
     * Loading files from a given directory by using [postFromName] for each file.
     *
     * @param dir the directory to take the files from
     * @return a list of loaded posts
     */
    private fun loadDownloadedPostListFromFile(dir: File): MutableList<DownloadedPost> {
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
        val idIndex = if (postFileName.split(" ")[1] == "-") 2 else 1

        val id = postFileName.split(" ")[idIndex].toLong()
        val source = postFileName.split(" ")[0].toLowerCase()

        return DownloadedPost(id = id, file = postFile, boardName = source)
    }

    /**
     * Asynchronously downloads a given post from a given url into [shinobooruImageDir].
     * The name of the file will be composed like:
     * '<board>/<board> <postId> <tags>.<dataType>'
     *
     * @param url to load from
     * @param post to get the id and tags from
     */
    fun downloadFileToStorage(url: String, post: Post): String? {
        checkExternalStorage()
        val pattern = Pattern.compile("http[s]?://(?:files\\.)?([a-z.]*)")
        val matcher = pattern.matcher(url)
        matcher.find()
        val board = matcher.group(1)

        if (boards[board]?.find { it.id == post.id } != null) {
            return "Post already exists"
        }

        val dataType = url.split(".").last()
        val fileName = "$board ${post.id} ${post.tags}.$dataType"
        val boardSubDir = File(shinobooruImageDir, board)

        boardSubDir.mkdirs()

        url.httpDownload().destination { res, realUrl ->

            Timber.i("dest = $boardSubDir/$fileName")

            //next line will be used as destination file
            File(boardSubDir, fileName)
        }.response { request, response, result ->
            if (result.component2() != null)
                Timber.d("Error = ${result.component2()}")
            else
                boards[board]?.add(postFromName(fileName, File(boardSubDir, fileName)))
        }

        return null
    }

    /** Check for Storage availability */
    private fun checkExternalStorage() {
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
    fun getDownloadedPosts(board: String): List<DownloadedPost>? = boards[board]

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
    fun fileById(board: String, id: Long): File? = boards[board]?.filter { it.id == id }?.first()?.file

    /**
     * Return a [FileInputStream] of the cached preview image or null if not cached.
     *
     * @param id of the post the preview belongs to
     * @return a [FileInputStream] or null
     */
    fun previewBitmapFromCache(board: String, id: Long): FileInputStream? =
            if (isPreviewBitmapCached(board, id))
                appContext.openFileInput("$board $id.jpeg")
            else
                null

    fun isPreviewBitmapCached(board: String, id: Long) = cachedFiles.contains("$board $id.jpeg")

    /**
     * Stores a given [Bitmap] with the given id as name
     * CompressFormat is png
     *
     * @param id for the file name
     * @param bitmap to store
     */
    fun previewBitmapToCache(board: String, id: Long, bitmap: Bitmap?) {
        val fos = appContext.openFileOutput("$board $id.jpeg", Context.MODE_PRIVATE)
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 85, fos)
        fos.close()
        cachedFiles.add("$id")
        checkCacheSize()
    }

    private fun checkCacheSize() {
        if (checkingCache)
            return
        if (cachedFiles.size > maxCachedSize) {
            checkingCache = true
            Timber.i("clean up cache")
            val list = appContext.filesDir.list().filter { it.endsWith(".jpeg") }
            list.sortedBy { File(it).lastModified() }
            list.drop(500).forEach { File(appContext.filesDir, it).delete() }

            cachedFiles.clear()
            cachedFiles.addAll(appContext.fileList())
            checkingCache = false
        }
    }

    fun deleteDownloadedPost(post: DownloadedPost) {
        val deleteResult = post.file.delete()
        boards[post.boardName]!!.remove(post)
    }
}