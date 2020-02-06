package com.github.firenox89.shinobooru.repo.model

import android.graphics.BitmapFactory
import java.io.File


class DownloadedPost(id: Long, val file: File, val boardName: String) : Post(id = id) {

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
        fun postFromName(postFile: File): DownloadedPost {
            //TODO: handle non posts

            val postFileName = postFile.name

            //compatibility with mbooru saved posts
            val idIndex = if (postFileName.split(" ")[1] == "-") 2 else 1

            val id = postFileName.split(" ")[idIndex].toLong()
            val source = postFileName.split(" ")[0].toLowerCase()

            return DownloadedPost(id = id, file = postFile, boardName = source)
        }
    }

    override fun getBoard(): String {
        return boardName
    }

    override fun toString(): String {
        return "Downloaded${super.toString()}, file = $file"
    }
}
