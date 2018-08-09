package com.github.firenox89.shinobooru.utility

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.github.firenox89.shinobooru.app.Shinobooru
import com.github.firenox89.shinobooru.model.Post
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Singleton class for handling API requests, currently only post and tag requests */
object ApiWrapper {
    private val TAG = "ApiWrapper"
    /** Scheduler to run post requests */
    private val requestScheduler = Schedulers.from(Executors.newCachedThreadPool())
    /** Post request queue */
    private val requestQueue = PublishSubject.create<Request>()
    /** Time in ms to wait between api calls */
    private var throttle = 10L

    /** Main constructor to set the request queue */
    init {
        //TODO: check if internet is available
        //yande.re does not talk to android user agents...
        FuelManager.instance.baseHeaders = mapOf("User-Agent" to "Java/1.8.0_112")
        requestQueue.throttleFirst(throttle, TimeUnit.MILLISECONDS, requestScheduler).
                subscribe {
                    val getR = it.request.httpGet()
                    // PostDeserializer will convert Inputstreams to an array of posts
                    getR.responseObject(Post.PostDeserializer()) { req, res, result ->
                        val (post, err) = result
                        if (err != null) {
                            Log.e(TAG, "Http request error $err", err.exception)
                            Log.e(TAG, "Http response ${String(err.response.data)}")
                        } else if (post != null) {
                            it.handler(post)
                        } else {
                            Log.e(TAG, "Something went wrong here")
                        }
                    }
                }
    }

    /** Check for Network availability */
    fun isOnline(): Boolean {
        val cm = Shinobooru.appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        return netInfo.isConnectedOrConnecting
    }

    /**
     * Build a [Request] and put it into the [requestQueue].
     *
     * @param board were the request is directed to
     * @param page number of the desired page for paging
     * @param tags to search for
     * @param limit for the number of post that should be return for this request
     * @param handler that will be called when the request was turned into an array of posts
     *
     */
    fun request(board: String,
                page: Int,
                tags: String = "",
                limit: Int = 20,
                handler: (Array<Post>) -> Unit) {
        //TODO: add board dependent request limits, so that we can stop before the board will stop us
        val params = "?limit=$limit${if (page > 1) "&page=$page" else ""}" +
                if (tags != "") "&tags=$tags" else ""
        val request = "$board/post.json$params"

        requestQueue.onNext(Request(request, handler))
    }

    /**
     * Request tag information for a given board and tag name.
     * Executes synchronously.
     *
     * @param board were the request is directed to
     * @param name of the tag to request information about
     *
     * @return Response as a string or null if something has gone wrong
     */
    fun requestTag(board: String, name: String): String? {
        Log.i(TAG, "Request tag info from board $board with name $name")
        var jsonResponse: String? = null
        //add protocol if it is missing
        val requestString = "${if (board.startsWith("http")) "" else "https://"}$board/tag.json?name=$name&limit=0"
        val (request, response, result) = requestString.httpGet()
                .header(mapOf("User-Agent" to "Java/1.8.0_92")).responseString()
        val (tag, err) = result
        if (tag != null) {
            jsonResponse = tag
        } else {
            Log.e(TAG, "Http request error $err")
        }
        return jsonResponse
    }

    /**
     * Data class to store post request for queueing
     *
     * @param request the request string, including all parameters
     * @param handler will be called when request was successfully loaded
     */
    data class Request(val request: String, val handler: (Array<Post>) -> Unit)
}