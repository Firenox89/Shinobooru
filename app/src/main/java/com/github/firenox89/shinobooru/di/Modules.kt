package com.github.firenox89.shinobooru.di

import android.preference.PreferenceManager
import com.github.firenox89.shinobooru.cloud.CloudSync
import com.github.firenox89.shinobooru.cloud.GoogleDrive
import com.github.firenox89.shinobooru.repo.DataSource
import com.github.firenox89.shinobooru.repo.DefaultDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module.module

val AppModule = module {
        single { PreferenceManager.getDefaultSharedPreferences(androidContext()) }
        single<DataSource> { DefaultDataSource() }
        single<CloudSync> { GoogleDrive(androidContext()) }
    }

val appModules = listOf(AppModule)
