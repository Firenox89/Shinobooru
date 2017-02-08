package com.github.firenox89.shinobooru.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.jetbrains.anko.doAsync
import java.io.File

/**
 * Created by firenox on 2/8/17.
 */

class DownloadedPost(id: Long, val file: File) : Post(id = id) {

    private val MAX_FILE_SIZE = 5L * 1024L * 1024L

    override fun loadSample(handler: (Bitmap?) -> Unit) {
        doAsync {
            val options = BitmapFactory.Options()
            //sample huge images
            if (file.length() > MAX_FILE_SIZE) {
                options.inSampleSize = 2
                options.inDither = true
                options.inPreferQualityOverSpeed = true
            }
            handler.invoke(BitmapFactory.decodeFile(file.path, options))
        }
    }

    override fun loadPreview(handler: (Bitmap?) -> Unit) {
        doAsync {
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

    override fun toString(): String {
        return "Downloaded${super.toString()}, file = $file"
    }
}
