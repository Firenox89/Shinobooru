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

    var url: String = SettingsActivity.currentBoardURL
    val json: Boolean = true
    var currentPage = 1
    private val requestScheduler = Schedulers.from(Executors.newCachedThreadPool())
    private val requestQueue = PublishSubject<Request>()
    private var throttle = 10L

    init {
        //TODO: check if internet is available
        FuelManager.instance.baseHeaders = mapOf("User-Agent" to "Java/1.8.0_92")
        requestQueue.throttleFirst(throttle, TimeUnit.MILLISECONDS, requestScheduler).
                subscribe {
                    //if we set baseURL beforehand, simply use relativePath
                    val params = "?limit=${it.limit}${if (it.page > 1) "&page=${it.page}" else ""}" +
                            "${if (it.tags != "") "&tags=$it.tags" else ""}"
                    var requestString = "$url/post${if (json) ".json" else ".xml"}$params"

                    currentPage++
                    val getR = requestString.httpGet()
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

    fun setBaseURL(url: String) {
        this.url = url
        when (url) {
            SettingsActivity.yandereURL -> throttle = 10
            SettingsActivity.konachanURL -> throttle = 300
        }
    }

    fun onRefresh() {
        currentPage = 1
    }

    //TODO: request limits
    fun request(limit: Int = 20, page: Int = currentPage, tags: String = "", handler: (Array<Post>?) -> Unit) {
        requestQueue.onNext(Request(limit, page, tags, handler))
    }

    data class Request(val limit: Int,
                       val page: Int,
                       val tags: String,
                       val handler: (Array<Post>?) -> Unit)
}