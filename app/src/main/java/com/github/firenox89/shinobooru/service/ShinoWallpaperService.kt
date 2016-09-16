package com.github.firenox89.shinobooru.service

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.github.firenox89.shinobooru.model.FileManager
import com.github.firenox89.shinobooru.model.Post
import com.github.salomonbrys.kodein.KodeinInjected
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import org.jetbrains.anko.doAsync
import rx.lang.kotlin.PublishSubject
import java.util.concurrent.TimeUnit

class ShinoWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return ShinoWallpaperEngine()
    }

    companion object {
        fun setWallpaperService(context: Context) {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(context, ShinoWallpaperService::class.java))
            context.startActivity(intent)
        }
    }

    inner class ShinoWallpaperEngine() : Engine(), KodeinInjected {
        override val injector = KodeinInjector()
        val pref: SharedPreferences by instance()
        //TODO: make the wallpaperService more configurable

        private var displayWidth: Int = 0
        private var displayHeight: Int = 0

        val list = mutableListOf<Post>()

        private val clickEventStream = PublishSubject<MotionEvent>()

        init {
            inject(appKodein())

            FileManager.boards.forEach { list.addAll(it.value) }

            clickEventStream.buffer(800, TimeUnit.MILLISECONDS).forEach {
                when (it.size) {
                    2 -> draw()
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            draw()
        }

        override fun onTouchEvent(event: MotionEvent?) {
            if (event?.action == MotionEvent.ACTION_UP) {
                clickEventStream.onNext(event)
            }
            super.onTouchEvent(event)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            displayWidth = width
            displayHeight = height
            draw()
            super.onSurfaceChanged(holder, format, width, height)
        }

        private fun draw() {
            doAsync {
                val holder = surfaceHolder
                var canvas: Canvas? = null
                try {
                    val image = pickImage()
                    val transformationInfo = calcTransformation(image)
                    val paint = Paint()
                    paint.color = 0x000000
                    paint.alpha = 255
                    //TODO: check if a previous job is still holing the lock
                    canvas = holder.lockCanvas()
                    canvas.drawPaint(paint)
                    canvas.translate(transformationInfo.second.x.toFloat() / 2,
                            transformationInfo.second.y.toFloat() / 2)
                    canvas.drawBitmap(image, transformationInfo.first, null)
                } finally {
                    if (canvas != null)
                        holder.unlockCanvasAndPost(canvas)
                }
            }
        }

        private fun calcTransformation(image: Bitmap): Pair<Matrix, Point> {
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

            val scaleX = finalWidth.toFloat() / width.toFloat()
            val scaleY = finalHeight.toFloat() / height.toFloat()

            val matrix = Matrix()

            matrix.postScale(scaleX, scaleY)

            return Pair(matrix, Point(displayWidth - finalWidth, displayHeight - finalHeight))
        }

        private fun pickImage(): Bitmap {
            var post = pickRandomPost()
            var bounds = getBounds(post.file!!.absolutePath)
            if (displayWidth > displayHeight) {
                while (bounds.first < bounds.second) {
                    post = pickRandomPost()
                    bounds = getBounds(post.file!!.absolutePath)
                }
            } else {
                while (bounds.first > bounds.second) {
                    post = pickRandomPost()
                    bounds = getBounds(post.file!!.absolutePath)
                }
            }
            return BitmapFactory.decodeFile(post.file?.absolutePath)
        }

        private fun pickRandomPost(): Post {
            var i = (Math.random() * list.size).toInt()
            return list[i]
        }

        private fun getBounds(path: String): Pair<Int, Int> {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true

            BitmapFactory.decodeFile(path, options)

            return Pair(options.outWidth, options.outHeight)
        }
    }
}
