package com.github.firenox89.shinobooru.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import com.github.firenox89.shinobooru.app.Shinobooru
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.windowManager
import java.io.File

/**
 * Created by firenox on 2/8/17.
 */

class DownloadedPost(id: Long, val file: File, val boardName: String) : Post(id = id) {

    init {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(file.path, options)
        height = options.outHeight
        width = options.outWidth
        file_size = file.length().toInt()
        val tagsStartIndex = file.name.indexOf(' ', file.name.indexOf(' ')+1)
        tags = file.name.substring(tagsStartIndex+1, file.name.length-4)
    }

    override fun loadSample(handler: (Bitmap?) -> Unit) {
        doAsync(Throwable::printStackTrace) {
            val display = Shinobooru.appContext.windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            //sample huge images
            handler.invoke(loadSubsampledImage(file, size.x, size.y))
        }
    }

    override fun loadPreview(handler: (Bitmap?) -> Unit) {
        doAsync(Throwable::printStackTrace) {
            handler.invoke(loadSubsampledImage(file, 250, 400))
        }
    }

    /**
     * Loads a sub sampled imaae from a given file.
     * The sample rate gets determined by the given width and height and the image size.
     */
    private fun loadSubsampledImage(file: File, reqWidth: Int, reqHeight: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(file.path, options)
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqWidth || width > reqHeight) {

            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqWidth && halfWidth / inSampleSize >= reqHeight) {
                inSampleSize *= 2
            }
        }
        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        options.inDither = true
        options.inPreferQualityOverSpeed = true

        return BitmapFactory.decodeFile(file.path, options)
    }

    override fun getBoard(): String {
        return boardName
    }

    override fun toString(): String {
        return "Downloaded${super.toString()}, file = $file"
    }
}
