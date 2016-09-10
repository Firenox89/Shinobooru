package com.github.firenox89.shinobooru.service

import android.app.Service
import android.app.WallpaperManager
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
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
import com.github.firenox89.shinobooru.receiver.ScreenEventReceiver
import com.github.salomonbrys.kodein.KodeinInjected
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import rx.Subscription
import rx.subjects.PublishSubject

class WallpaperService : Service(), KodeinInjected {

    companion object {
        lateinit var instance: WallpaperService
            private set
    }
    override val injector = KodeinInjector()
    val pref: SharedPreferences by instance()
    val display: Display by instance()
    val screenEventStream = PublishSubject.create<Intent>()
    val wallpaperManager = WallpaperManager.getInstance(Shinobooru.appContext)

    val list = mutableListOf<Post>()

    var displaySize = Point()
    var subscription: Subscription? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        inject(appKodein())
        instance = this

        val intentfilter = IntentFilter()
        intentfilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED)
        intentfilter.addAction(Intent.ACTION_SCREEN_ON)

        registerReceiver(ScreenEventReceiver(screenEventStream), intentfilter)

        val size = Point()
        display.getSize(size)
        FileManager.boards.forEach { list.addAll(it.value) }
        enableWallpaperService(pref.getBoolean("enable_wallpaper", true))

        return super.onStartCommand(intent, flags, startId)
    }

    fun subscribeToScreenEvents() {
        subscription = screenEventStream.subscribe {
            if (!it.action.equals("android.intent.action.CONFIGURATION_CHANGED") || hasOrientationChanged()) {
                if (display.rotation == Surface.ROTATION_0 ||
                        display.rotation == Surface.ROTATION_180)
                    wallpaperManager.setBitmap(pickWideImage())
                else
                    wallpaperManager.setBitmap(pickHighImage())
            }
        }
    }

    fun unsubscribeToScreenEvents() {
        subscription?.unsubscribe()
        subscription = null
    }

    fun enableWallpaperService(enable: Boolean) {
        if (enable && subscription == null) {
            subscribeToScreenEvents()
        } else {
            unsubscribeToScreenEvents()
        }
    }

    fun hasOrientationChanged(): Boolean {
        val size = Point()
        display.getSize(size)
        val result = displaySize.equals(size)
        displaySize = size
        return result
    }

    fun pickHighImage(): Bitmap {
        var bitmap = pickImage()
        while (bitmap.width > bitmap.height) {
            bitmap = pickImage()
        }
        return resize(bitmap)
    }

    fun pickWideImage(): Bitmap {
        var bitmap = pickImage()
        while (bitmap.width < bitmap.height) {
            bitmap = pickImage()
        }
        return resize(bitmap)
    }

    fun pickImage(): Bitmap {
        var i = (Math.random() * list.size).toInt()
        var post = list[i]
        return BitmapFactory.decodeFile(post.file?.absolutePath)
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

        var finalWidth = displayWidth
        var finalHeight = displayHeight
        if (ratioMax > 1) {
            finalWidth = (displayHeight.toFloat() * ratioBitmap).toInt()
        } else {
            finalHeight = (displayWidth.toFloat() / ratioBitmap).toInt()
        }
        return addPading(Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true))
    }

    fun addPading(wallpaper: Bitmap): Bitmap {

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
