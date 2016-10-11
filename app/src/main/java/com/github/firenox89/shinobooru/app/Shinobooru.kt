package com.github.firenox89.shinobooru.app

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Point
import android.preference.PreferenceManager
import android.view.Display
import com.github.salomonbrys.kodein.*
import org.jetbrains.anko.windowManager
import rx.lang.kotlin.PublishSubject
import rx.subjects.PublishSubject

/**
 * Application class, provide the [appContext] for convenience, initialize [Kodein].
 */
class Shinobooru : Application(), KodeinAware {

    override val kodein by Kodein.lazy {
        bind<SharedPreferences>() with instance(PreferenceManager.getDefaultSharedPreferences(appContext))

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
    }

    companion object {
        /**
         * The application context.
         */
        lateinit var appContext: Context
            private set
    }
}
