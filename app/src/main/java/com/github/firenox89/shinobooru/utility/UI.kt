package com.github.firenox89.shinobooru.utility

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.github.kittinunf.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

object UI {
    /**
     * Loads a sub sampled imaae from a given file.
     * The sample rate gets determined by the given width and height and the image size.
     */
    suspend fun loadSubsampledImage(file: File, reqWidth: Int, reqHeight: Int): Result<Bitmap, Exception> = withContext(Dispatchers.IO) {
        Result.of<Bitmap, Exception> {
            Timber.v("Load image from '$file'")
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

            BitmapFactory.decodeFile(file.path, options)
        }
    }
}