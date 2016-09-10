package com.github.firenox89.shinobooru.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import com.github.firenox89.shinobooru.app.Shinobooru
import com.github.kittinunf.fuel.httpDownload
import java.io.File
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

object FileManager {

    val shinobooruImageDir = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES), "shinobooru")

    //TODO: use sparseArray, maybe
    val boards = mutableMapOf<String, List<Post>>()
    val cachedFiles = mutableListOf<String>()

    init {
        checkExternalStorage()
        shinobooruImageDir.mkdirs()
        shinobooruImageDir.listFiles().forEach { boards.put(it.name, loadPostListFromFile(it)) }

        cachedFiles.addAll(Shinobooru.appContext.fileList())
    }

    private fun loadPostListFromFile(dir: File): List<Post> {
        val postList = mutableListOf<Post>()

        dir.listFiles().forEach { postList.add(postFromFile(it)) }
        return postList
    }

    private fun postFromFile(postFile: File): Post {
        //TODO: handle non post
        //compatibility with mbooru saved posts
        val idIndex = if (postFile.name.split(" ")[1].equals("-")) 2 else 1

        val id = postFile.name.split(" ")[idIndex].toLong()
        val source = postFile.name.split(" ")[0].toLowerCase()

        return Post(id = id, fileSource = source, file = postFile)
    }

    fun downloadFileToStorage(url: String, post: Post) {
        checkExternalStorage()
        url.httpDownload().destination { res, realUrl ->
            val boardSubDirName = getImageSource()
            val dataType = url.split(".").last()
            val fileName = "${getImageSource()} ${post.id} ${post.tags}.$dataType"
            val boardSubDir = File(shinobooruImageDir, boardSubDirName)

            boardSubDir.mkdirs()

            Log.d("Download", "dest = $shinobooruImageDir/$fileName")

            //next line will be used as destination file
            File(boardSubDir, fileName)
        }.response { request, response, result ->
            //TODO: there have to be a nicer way to kick this off
            Log.d("Download", "Error = ${result.component2()}")
        }
    }

    fun checkExternalStorage() {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            throw IOException("External storage not mounted")
        }
    }

    fun getImageSource(): String {
        //TODO: parse that from url
        return PostLoader.instance.getCurrentURL().replace(Regex("http[s]?://"), "")
    }

    fun getPosts(): List<Post>? {
        return boards[getImageSource()]
    }

    fun fileById(id: Long): File? {
        return boards[getImageSource()]?.filter { it.id == id }?.first()?.file
    }

    fun previewBitmapFromCache(id: Long): Bitmap? {
        if (cachedFiles.contains("$id"))
            return BitmapFactory.decodeStream(Shinobooru.appContext.openFileInput("$id"))
        else
            return null
    }

    fun previewBitmapToCache(id: Long, it: Bitmap?) {
        val fos = Shinobooru.appContext.openFileOutput("$id", Context.MODE_PRIVATE)
        it?.compress(Bitmap.CompressFormat.PNG, 85, fos)
        fos.close()
        cachedFiles.add("$id")
    }

    fun syncPosts() {
        //TODO: implement
    }

    fun saveViewedList(viewedList: MutableList<Long>) {
        val outputStream = Shinobooru.appContext.openFileOutput("ViewList", Context.MODE_PRIVATE)
        ObjectOutputStream(outputStream).apply { writeObject(viewedList) }.close()
    }

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