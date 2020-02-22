package com.github.firenox89.shinobooru.di

import android.preference.PreferenceManager
import com.github.firenox89.shinobooru.cloud.CloudSync
import com.github.firenox89.shinobooru.cloud.NextCloudSyncer
import com.github.firenox89.shinobooru.repo.*
import com.github.firenox89.shinobooru.settings.SettingsManager
import com.github.firenox89.shinobooru.ui.sync.SyncViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val DataModul = module {
    single { ApiWrapper(androidContext()) }
    single { FileManager(androidContext()) }
    single { LocalPostLoader(androidContext(), get()) }
    single<DataSource> { DefaultDataSource(get(), get(), get()) }
    single { NextCloudSyncer(androidContext(), get()) }
    single { SyncViewModel(get(), get()) }
}

val AppModule = module {
    single { SettingsManager(PreferenceManager.getDefaultSharedPreferences(androidContext())) }
}

val appModules = listOf(AppModule, DataModul)
