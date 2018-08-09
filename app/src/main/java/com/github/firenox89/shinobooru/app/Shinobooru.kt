package com.github.firenox89.shinobooru.app

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Build
import android.preference.PreferenceManager
import android.view.Display
import com.github.salomonbrys.kodein.*
import io.reactivex.subjects.PublishSubject
import org.jetbrains.anko.windowManager

/**
 * Application class, provide the [appContext] for convenience, initialize [Kodein].
 */
class Shinobooru : Application(), KodeinAware {

    override val kodein by Kodein.lazy {
        bind<SharedPreferences>() with instance(PreferenceManager.getDefaultSharedPreferences(appContext))

        bind<PublishSubject<Int>>("thumbnailUpdates") with singleton { PublishSubject.create<Int>() }

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
