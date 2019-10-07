package com.github.firenox89.shinobooru.di

import android.preference.PreferenceManager
import com.github.firenox89.shinobooru.cloud.CloudSync
import com.github.firenox89.shinobooru.cloud.GoogleDrive
import com.github.firenox89.shinobooru.repo.*
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val DataModul = module {
    single { ApiWrapper(androidContext()) }
    single { FileManager(androidContext()) }
    single { FileLoader(androidContext(), get()) }
    single<DataSource> { DefaultDataSource(get(), get(), get()) }
    single<CloudSync> { GoogleDrive(androidContext(), get()) }
}

val AppModule = module {
    single { PreferenceManager.getDefaultSharedPreferences(androidContext()) }
}

val appModules = listOf(AppModule, DataModul)
