package com.github.firenox89.shinobooru.app

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Point
import android.preference.PreferenceManager
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.Display
import com.github.firenox89.shinobooru.service.WallpaperService
import com.github.firenox89.shinobooru.ui.PostRecyclerAdapter
import com.github.salomonbrys.kodein.*
import org.jetbrains.anko.windowManager
import rx.lang.kotlin.PublishSubject
import rx.subjects.PublishSubject

class Shinobooru : Application(), KodeinAware {

    override val kodein by Kodein.lazy {
        bind<SharedPreferences>() with instance(PreferenceManager.getDefaultSharedPreferences(appContext))

        bind<StaggeredGridLayoutManager>() with singleton { StaggeredGridLayoutManager(4, StaggeredGridLayoutManager.VERTICAL) }
        bind<PostRecyclerAdapter>() with singleton { PostRecyclerAdapter() }
        bind<PublishSubject<Int>>("thumbnailUpdates") with singleton { PublishSubject<Int>() }

        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        bind<Display>() with instance(display)
        bind<Int>("width") with instance(size.x)
        bind<Int>("height") with instance(size.y)
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext

        val intent = Intent(this, WallpaperService::class.java)
        startService(intent)
    }

    companion object {
        lateinit var appContext: Context
            private set
    }
}
