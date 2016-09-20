package com.github.firenox89.shinobooru.model

import android.util.Log
import com.github.firenox89.shinobooru.settings.SettingsActivity
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import rx.lang.kotlin.PublishSubject
import rx.schedulers.Schedulers
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object ApiWrapper {
    private val requestScheduler = Schedulers.from(Executors.newCachedThreadPool())
    private val requestQueue = PublishSubject<Request>()
    private var throttle = 10L

    init {
        //TODO: check if internet is available
        FuelManager.instance.baseHeaders = mapOf("User-Agent" to "Java/1.8.0_92")
        requestQueue.throttleFirst(throttle, TimeUnit.MILLISECONDS, requestScheduler).
                subscribe {

                    val getR = it.request.httpGet()
                    getR.responseObject(Post.Deserializer()) { req, res, result ->
                        //result is of type Result<User, Exception>
                        val (post, err) = result
                        if (err != null) {
                            Log.e("Http request error", "$err")
                        } else {
                            it.handler(post)
                        }
                    }
                }
    }

    //TODO: request limits
    fun request(board: String,
                page: Int,
                tags: String = "",
                limit: Int = 20,
                handler: (Array<Post>?) -> Unit) {
        val params = "?limit=$limit${if (page > 1) "&page=$page" else ""}" +
                "${if (tags != "") "&tags=$tags" else ""}"
        var request = "$board/post.json$params"

        requestQueue.onNext(Request(request, handler))
    }

    data class Request(val request: String, val handler: (Array<Post>?) -> Unit)
}