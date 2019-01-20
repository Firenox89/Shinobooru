package com.github.firenox89.shinobooru.di

import android.preference.PreferenceManager
import com.github.firenox89.shinobooru.repo.model.DefaultDataSource
import io.reactivex.subjects.PublishSubject
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module.module

val AppModule = module {
        single { PreferenceManager.getDefaultSharedPreferences(androidContext()) }
        single { DefaultDataSource() }

        single(name = "thumbnailUpdates") { PublishSubject.create<Int>() }
    }

val appModules = listOf(AppModule)
