package com.github.firenox89.shinobooru.repo

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import com.github.firenox89.shinobooru.image.meta.ImageMetadataPostWriter
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
    class PostAlreadyDownloaded : Exception() {
        override val message: String?
            get() = "already downloaded."
    }
}

class FileManager(private val appContext: Context) {

    /** The image directory inside androids picture dir. */
    val shinobooruImageDir = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES), "shinobooru")

    val shinobooruSyncDir = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES), "shinobooru-sync")

    private val downloadedPosts: MutableList<DownloadedPost> by lazy {
        loadDownloadedPostsFromDir()
    }

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

        // load list of files from cache
        cachedFiles.addAll(appContext.fileList())
    }

    private fun loadDownloadedPostsFromDir(): MutableList<DownloadedPost> {
        val list = mutableListOf<DownloadedPost>()
        shinobooruImageDir.listFiles().forEach { file ->
            if (file.isFile) {
                postFromName(file).fold({
                    list.add(it)
                }, { exception -> Timber.w(exception, "Failed to load $file") })
            }
        }
        Timber.d("Loading posts from storage complete")
        return list
    }

    fun getDownloadDestinationFor(post: Post): Result<File, Exception> =
            isExternalStorageMounted().map {
                val url = post.file_url
                val board = post.getBoard()

                if (downloadedPosts.find { it.id == post.id } != null) {
                    throw FileManagerExecptions.PostAlreadyDownloaded()
                } else {
                    val dataType = url.split(".").last()
                    val fileName = "${board}_${post.id}.$dataType"
                    File(shinobooruImageDir, fileName)
                }
            }

    fun getDownloadDestinationFor(fileName: String): File = File(shinobooruImageDir, fileName)

    fun getSyncDir(): File = shinobooruSyncDir

    private fun isExternalStorageMounted(): Result<Unit, IOException> =
            if (Environment.MEDIA_MOUNTED != Environment.getExternalStorageState()) {
                Result.error(IOException("External storage not mounted"))
            } else {
                Result.success(Unit)
            }

    fun getAllDownloadedPosts(reload: Boolean = false): List<DownloadedPost> {
        if (reload) {
            downloadedPosts.clear()
            downloadedPosts.addAll(loadDownloadedPostsFromDir())
        }
        return downloadedPosts
    }

    /**
     * Returns a post from the download lists for the given board name and post id
     *
     * @param board name for the list
     * @param id of the post
     * @return [DownloadedPost] or null
     */
    fun fileById(board: String, id: Long): File? = downloadedPosts.firstOrNull { it.id == id }?.file

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
    fun previewBitmapToCache(board: String, id: Long, bitmap: Bitmap) {
        val fos = appContext.openFileOutput("$board $id.jpeg", Context.MODE_PRIVATE)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
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

    fun deleteDownloadedPost(post: DownloadedPost): Result<Boolean, Exception> =
            Result.of {
                downloadedPosts.remove(post)
                post.file.delete()
            }

    fun writeMetadataInFileAndAddToList(post: Post, file: File): Result<Unit, Exception> {
        return Result.of<Unit, Exception> {
            val metaPost = com.github.firenox89.shinobooru.image.meta.Post(
                    post.getBoard(),
                    post.id.toString(),
                    post.author,
                    post.source,
                    post.rating,
                    post.tags)
            Timber.w("Write post to image $post $file")
            val res = ImageMetadataPostWriter.writePostToImage(file, file, metaPost)
            Timber.w("res $res")
        }.flatMap {
            Timber.w("$it")
            postFromName(file).map { downloadedPost ->
                Timber.w("add post to downloaded list $downloadedPost")
                addToList(downloadedPost)
                Unit
            }
        }
    }

    fun addToList(post: DownloadedPost) {
        downloadedPosts.add(post)
    }
}