package com.github.firenox89.shinobooru.repo

import android.content.Context
import android.net.ConnectivityManager
import com.github.firenox89.shinobooru.app.Shinobooru
import com.github.firenox89.shinobooru.repo.model.Post
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.coroutines.awaitObject
import com.github.kittinunf.fuel.coroutines.awaitObjectResult
import com.github.kittinunf.fuel.coroutines.awaitString
import com.github.kittinunf.fuel.httpGet
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import timber.log.Timber
import java.lang.IllegalStateException

class ApiWrapper(private val appContext: Context) {
    init {
        //TODO: check if internet is available
        //yande.re does not talk to android user agents...
        FuelManager.instance.baseHeaders = mapOf("User-Agent" to "Java/1.8.0_112")
    }

    /** Check for Network availability */
    fun isOnline(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        return netInfo.isConnectedOrConnecting
    }

    suspend fun request(board: String,
                        page: Int,
                        tags: String = "",
                        limit: Int = 20): Array<Post> =
            //TODO: add board dependent request limits, so that we can stop before the board will stop us
            buildPostRequest(board, page, tags, limit).also { Timber.d("request '$it'") }.httpGet().awaitObject(PostDeserializer())


    suspend fun requestTag(board: String, name: String): String =
            //add protocol if it is missing
            "${board.prepentHttp()}/tag.json?name=$name&limit=0"
                    .httpGet().awaitString().also {
                        Timber.i("Request tag info from board $board with name $name")
                    }


    private fun String.prepentHttp(): String = if (this.startsWith("http")) this else "https://$this"

    private fun buildPostRequest(board: String, page: Int, tags: String, limit: Int): String =
            "${board.prepentHttp()}/post.json?limit=$limit" +
                    (if (page > 1) "&page=$page" else "") +
                    (if (tags != "") "&tags=$tags" else "")


    class PostDeserializer : ResponseDeserializable<Array<Post>> {
        override fun deserialize(content: String): Array<Post> = Gson().fromJson(content, Array<Post>::class.java)
    }

}