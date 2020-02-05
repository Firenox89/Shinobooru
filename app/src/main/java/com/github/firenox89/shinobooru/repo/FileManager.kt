package com.github.firenox89.shinobooru.repo

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import com.github.firenox89.shinobooru.repo.model.DownloadedPost
import com.github.firenox89.shinobooru.repo.model.DownloadedPost.Companion.postFromName
import com.github.firenox89.shinobooru.repo.model.Post
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.lang.Exception

sealed class FileManagerExecptions {
    class PostAlreadyDownloaded : Exception()
}

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

    @Volatile
    private var checkingCache = false

    /** Initialize the cached and downloaded post lists. */
    init {
        //TODO don't throw this from the constructor
        isExternalStorageMounted().component2()?.let { throw it }
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

        dir.listFiles().forEach { postList.add(postFromName(it)) }
        return postList
    }

    fun getDownloadDestinationFor(post: Post): Result<File, Exception> =
            isExternalStorageMounted().map {
                val url = post.file_url
                val board = post.getBoard()

                if (boards[board]?.find { it.id == post.id } != null) {
                    throw FileManagerExecptions.PostAlreadyDownloaded()
                } else {
                    val dataType = url.split(".").last()
                    val fileName = "$board ${post.id} ${post.tags}.$dataType"
                    val boardSubDir = File(shinobooruImageDir, board)

                    boardSubDir.mkdirs()

                    File(boardSubDir, fileName)
                }
            }

    private fun isExternalStorageMounted(): Result<Unit, IOException> =
            if (Environment.MEDIA_MOUNTED != Environment.getExternalStorageState()) {
                Result.error(IOException("External storage not mounted"))
            } else {
                Result.success(Unit)
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

    fun addDownloadedPost(post: Post, file: File) {
        boards[post.getBoard()]?.add(postFromName(file))
    }
}