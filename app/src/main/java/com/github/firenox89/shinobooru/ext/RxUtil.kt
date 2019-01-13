package com.github.firenox89.shinobooru.ext

import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Publisher

fun <T> Observable<T>.ioSchedulers(): Observable<T> = this.compose(ApplyIOSchedulersTransformer())

fun <T> Flowable<T>.ioSchedulers(): Flowable<T> = this.compose(ApplyIOSFlowableTransformer())

fun <T> Observable<T>.defaultSchedulers(): Observable<T> = this.compose(ApplySchedulersTransformer())

fun <T> Flowable<T>.defaultSchedulers(): Flowable<T> = this.compose(ApplyFlowableSchedulersTransformer())

fun <T> Single<T>.defaultSchedulers(): Single<T> = this.compose(ApplySingleSchedulersTransformer())

fun <T> Observable<T>.uiSchedulers(): Observable<T> = this.compose(ApplyUiSchedulersTransformer())

fun <T> Flowable<T>.uiSchedulers(): Flowable<T> = this.compose(ApplyUiFlowableSchedulersTransformer())

class ApplyIOSchedulersTransformer<T : Any?> : ObservableTransformer<T, T> {
    override fun apply(upstream: Observable<T>): ObservableSource<T> {
        return upstream
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
    }
}

class ApplyIOSFlowableTransformer<T : Any?> : FlowableTransformer<T, T> {
    override fun apply(upstream: Flowable<T>): Publisher<T> {
        return upstream
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
    }
}

class ApplySchedulersTransformer<T : Any?> : ObservableTransformer<T, T> {
    override fun apply(source: Observable<T>): ObservableSource<T> {
        return source
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

}

class ApplyFlowableSchedulersTransformer<T : Any?> : FlowableTransformer<T, T> {
    override fun apply(upstream: Flowable<T>): Publisher<T> {
        return upstream
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }
}

class ApplySingleSchedulersTransformer<T : Any?> : SingleTransformer<T, T> {
    override fun apply(upstream: Single<T>): Single<T> {
        return upstream
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }
}

class ApplyUiSchedulersTransformer<T : Any?> : ObservableTransformer<T, T> {
    override fun apply(source: Observable<T>): ObservableSource<T> {
        return source
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
    }
}

class ApplyUiFlowableSchedulersTransformer<T : Any?> : FlowableTransformer<T, T> {
    override fun apply(upstream: Flowable<T>): Publisher<T> {
        return upstream
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
    }
}