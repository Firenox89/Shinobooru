package com.github.firenox89.shinobooru.repo.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.reactivex.Single
import java.io.File

/**
 * Created by firenox on 2/8/17.
 */

class DownloadedPost(id: Long, val file: File, val boardName: String) : Post(id = id) {

    init {
        if (file.exists()) {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(file.path, options)
            height = options.outHeight
            width = options.outWidth
            preview_height = height
            actual_preview_height = height
            jpeg_height = height
            preview_width = width
            actual_preview_width = width
            jpeg_width = width
            file_size = file.length().toInt()
            val tagsStartIndex = file.name.indexOf(' ', file.name.indexOf(' ') + 1)
            tags = file.name.substring(tagsStartIndex + 1, file.name.length - 4)
        }
    }

    override fun getBoard(): String {
        return boardName
    }

    override fun toString(): String {
        return "Downloaded${super.toString()}, file = $file"
    }
}
