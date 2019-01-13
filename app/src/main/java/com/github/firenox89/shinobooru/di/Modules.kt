package com.github.firenox89.shinobooru.di

import android.preference.PreferenceManager
import io.reactivex.subjects.PublishSubject
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module.module

val AppModule = module {
        single { PreferenceManager.getDefaultSharedPreferences(androidContext()) }

        single(name = "thumbnailUpdates") { PublishSubject.create<Int>() }
    }

val appModules = listOf(AppModule)
