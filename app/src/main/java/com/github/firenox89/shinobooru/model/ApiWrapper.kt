package com.github.firenox89.shinobooru.model

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.github.firenox89.shinobooru.app.Shinobooru
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
                        val (post, err) = result
                        if (err != null) {
                            Log.e("Http request error", "$err")
                        } else {
                            it.handler(post)
                        }
                    }
                }
    }

    fun isOnline(): Boolean {
        val cm = Shinobooru.appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        return netInfo.isConnectedOrConnecting
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

    fun requestTag(board: String, name: String): String {
        var jsonRespone = "Fail"
        val requestString = "https://$board/tag.json?name=$name"
        val (request, response, result) = requestString.httpGet()
                .header(mapOf("User-Agent" to "Java/1.8.0_92")).responseString()
        val (tag, err) = result
        if (tag != null) {
            jsonRespone = tag
        } else {
            Log.e("Tag", "Http request error $err")
        }
        return jsonRespone
    }

    data class Request(val request: String, val handler: (Array<Post>?) -> Unit)
}