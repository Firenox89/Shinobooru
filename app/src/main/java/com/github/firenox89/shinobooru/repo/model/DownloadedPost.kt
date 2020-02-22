package com.github.firenox89.shinobooru.repo.model

import android.graphics.BitmapFactory
import com.github.firenox89.shinobooru.image.meta.ImageMetadataPostWriter
import com.github.kittinunf.result.Result
import java.io.File
import java.lang.Exception
import java.lang.IllegalStateException


class DownloadedPost(id: Long, val file: File, private val boardName: String) : Post(id = id) {

    init {
        if (file.exists()) {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(file.path, options)
            height = options.outHeight
            width = options.outWidth
            preview_height = height //TODO calculate a reasonable preview height
            actual_preview_height = height
            jpeg_height = height
            preview_width = width //TODO calculate a reasonable preview width
            actual_preview_width = width
            jpeg_width = width
            file_size = file.length().toInt()
            val tagsStartIndex = file.name.indexOf(' ', file.name.indexOf(' ') + 1)
            val fileName = file.name.replace("[slash]", "/")
            tags = fileName.substring(tagsStartIndex + 1, fileName.length - 4)
        }
    }

    companion object {
        /**
         * Load a post from a file by parsing the file name to get the post id
         */
        fun postFromName(postFile: File): Result<DownloadedPost, Exception> =
                Result.of {
                    val post = ImageMetadataPostWriter.readPostFromImage(postFile)

                    DownloadedPost(id = post.id.toLong(), file = postFile, boardName = post.board).also {
                        it.tags = post.tags
                        it.author = post.author
                        it.source = post.source
                        it.rating = post.rating
                    }
                }

    }

    override fun getBoard(): String {
        return boardName
    }

    fun getMIMEType(): String =
            if (file.name.endsWith("jpg") || file.name.endsWith("jpeg")) {
                "image/jpeg"
            } else if (file.name.endsWith("png")) {
                "image/png"
            } else {
                throw IllegalStateException("Unsupported MIME type $file")
            }

    override fun toString(): String {
        return "Downloaded${super.toString()}, file = $file"
    }
}

class CloudPost(val remotePath: String, val fileName: String):
        Post(remotePath.split("_")[1].let { it.substring(0, it.length - 4) }.toLong()) {
    override fun getBoard(): String =
        fileName.split("_")[0]
}
