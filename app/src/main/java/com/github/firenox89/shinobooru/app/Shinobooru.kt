package com.github.firenox89.shinobooru.app

import android.app.Application
import com.github.firenox89.shinobooru.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

/**
 * Application class, initialize Timber and Koin.
 */
class Shinobooru : Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())

        startKoin{
            androidContext(applicationContext)
            modules(appModules)
        }
    }
}
