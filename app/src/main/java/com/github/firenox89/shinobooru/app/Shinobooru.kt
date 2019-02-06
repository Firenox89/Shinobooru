package com.github.firenox89.shinobooru.app

import android.app.Application
import android.content.Context
import com.github.firenox89.shinobooru.app.Shinobooru.Companion.appContext
import com.github.firenox89.shinobooru.di.appModules
import org.koin.android.ext.android.startKoin
import timber.log.Timber

/**
 * Application class, provide the [appContext] for convenience, initialize [Kodein].
 */
class Shinobooru : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext

        Timber.plant(Timber.DebugTree())

        startKoin(applicationContext, appModules)
    }

    companion object {
        /**
         * The application context.
         */
        lateinit var appContext: Context
            private set
    }
}
