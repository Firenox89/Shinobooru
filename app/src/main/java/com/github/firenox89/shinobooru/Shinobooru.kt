package com.github.firenox89.shinobooru

import android.app.Application
import com.github.firenox89.shinobooru.di.appModules
import io.realm.Realm
import io.realm.RealmConfiguration
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class Shinobooru : Application() {
    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())

        startKoin {
            androidContext(applicationContext)
            modules(appModules)
        }

        Realm.init(applicationContext)

        Realm.setDefaultConfiguration(RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build())
    }
}
