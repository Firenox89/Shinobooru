package com.github.firenox89.shinobooru.ui.base

import android.support.v7.app.AppCompatActivity
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

open class RxActivity: AppCompatActivity() {
    val disposeables = CompositeDisposable()

    fun subscribe(disposable: Disposable) {
        disposeables.add(disposable)
    }

    override fun onDestroy() {
        disposeables.dispose()
        super.onDestroy()
    }
}