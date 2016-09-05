package com.github.firenox89.shinobooru.service

import android.app.Service
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.IBinder
import android.util.Log
import android.view.Display
import android.view.Surface
import com.github.firenox89.shinobooru.app.Shinobooru
import com.github.firenox89.shinobooru.model.FileManager
import com.github.firenox89.shinobooru.model.Post
import com.github.salomonbrys.kodein.KodeinInjected
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance

class WallpaperService : Service(), KodeinInjected {

    override val injector = KodeinInjector()
    val display: Display by instance()

    val list = mutableListOf<Post>()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        inject(appKodein())

        val wallpaperManager = WallpaperManager.getInstance(Shinobooru.appContext)
        FileManager.boards.forEach { list.addAll(it.value) }

        Thread {
            while (true) {
                Thread.sleep(15 * 60 * 1000)
                wallpaperManager.setWallpaperOffsetSteps(1F, 1F)
                var bitmap: Bitmap
                if (display.rotation == Surface.ROTATION_0 ||
                        display.rotation == Surface.ROTATION_180)
                    bitmap = pickWideImage()
                else
                    bitmap = pickHighImage()
                wallpaperManager.setBitmap(bitmap)
            }
        }.start()
        return super.onStartCommand(intent, flags, startId)
    }

    fun pickHighImage(): Bitmap {
        val i = (Math.random() * list.size).toInt()
        val post = list[i]
        var bitmap = BitmapFactory.decodeFile(post.file?.absolutePath)
        if (bitmap.width > bitmap.height) {
            bitmap = null
            bitmap = pickHighImage()
        } else {
            resize(bitmap)
        }

        return bitmap
    }

    fun pickWideImage(): Bitmap {
        val i = (Math.random() * list.size).toInt()
        val post = list[i]
        var bitmap = BitmapFactory.decodeFile(post.file?.absolutePath)
        if (bitmap.width < bitmap.height) {
            bitmap = pickWideImage()
        }
        return resize(bitmap)
    }

    private fun resize(image: Bitmap): Bitmap {
        val size = Point()
        display.getSize(size)
        val displayWidth = size.x
        val displayHeight = size.y

        var image = image
        val width = image.width
        val height = image.height
        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = displayWidth.toFloat() / displayHeight.toFloat()
        Log.e("Wallp", "ratiobit $ratioBitmap ratimax $ratioMax display $displayWidth x $displayHeight")

        var finalWidth = displayWidth
        var finalHeight = displayHeight
        if (ratioMax > 1) {
            finalWidth = (displayHeight.toFloat() * ratioBitmap).toInt()
        } else {
            finalHeight = (displayWidth.toFloat() / ratioBitmap).toInt()
        }
        Log.e("Wallp", "$finalWidth $finalHeight")
        return addPading(Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true))
    }

    fun addPading(wallpaper: Bitmap): Bitmap {
        val wallpaperManager = WallpaperManager.getInstance(this)

        if (wallpaperManager.desiredMinimumWidth > wallpaper.width &&
                wallpaperManager.desiredMinimumHeight > wallpaper.height) {
            //add padding to wallpaper so background image scales correctly
            val xPadding = Math.max(0, wallpaperManager.desiredMinimumWidth - wallpaper.width) / 2
            val yPadding = Math.max(0, wallpaperManager.desiredMinimumHeight - wallpaper.height) / 2
            val paddedWallpaper = Bitmap.createBitmap(wallpaperManager.desiredMinimumWidth, wallpaperManager.desiredMinimumHeight, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(wallpaper.width * wallpaper.height)
            wallpaper.getPixels(pixels, 0, wallpaper.width, 0, 0, wallpaper.width, wallpaper.height)
            paddedWallpaper.setPixels(pixels, 0, wallpaper.width, xPadding, yPadding, wallpaper.width, wallpaper.height)

            return paddedWallpaper
        } else {
            return wallpaper
        }
    }
}
