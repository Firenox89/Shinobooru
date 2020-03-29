package com.github.firenox89.shinobooru.repo

import android.content.Context
import com.github.firenox89.shinobooru.repo.model.Post
import com.github.firenox89.shinobooru.repo.model.Tag
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.core.awaitResult
import com.github.kittinunf.fuel.coroutines.awaitObjectResult
import com.github.kittinunf.fuel.httpDownload
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import timber.log.Timber
import java.io.File

class ApiWrapper(private val appContext: Context) {
    init {
        //TODO: check if internet is available
        //yande.re does not talk to android user agents...
        FuelManager.instance.baseHeaders = mapOf("User-Agent" to "Java/1.8.0_112")
    }

    suspend fun requestPost(board: String,
                            page: Int,
                            tags: String = "",
                            limit: Int = 20): Result<List<Post>, FuelError> =
            //TODO: add board dependent request limits, so that we can stop before the board will stop us
            buildPostRequest(board, page, tags, limit).also { Timber.d("request '$it'") }.httpGet().awaitObjectResult(PostDeserializer)


    suspend fun requestTag(board: String, tagName: String): Result<List<Tag>, FuelError> =
            //add protocol if it is missing
            "${board.prepentHttp()}/tag.json?name=$tagName&limit=0".also {
                Timber.i("Request tag info from board '$board' with name '$tagName'. Request '$it'")
            }.httpGet().awaitObjectResult(TagDeserializer).also { Timber.d("Requested Tags $board, $tagName") }

    private fun String.prepentHttp(): String = if (this.startsWith("http")) this else "https://$this"

    private fun buildPostRequest(board: String, page: Int, tags: String, limit: Int): String =
            "${board.prepentHttp()}/post.json?limit=$limit" +
                    (if (page > 1) "&page=$page" else "") +
                    (if (tags != "") "&tags=$tags" else "")

    suspend fun downloadPost(post: Post, destination: File) =
            post.file_url.httpDownload().fileDestination { _, _ ->
                destination
            }.awaitResult(NoopDeserializer)

    object PostDeserializer : ResponseDeserializable<List<Post>> {
        override fun deserialize(content: String): List<Post> = Gson().fromJson(content, Array<Post>::class.java).toList()
    }

    object TagDeserializer : ResponseDeserializable<List<Tag>> {
        override fun deserialize(content: String): List<Tag> = Gson().fromJson(content, Array<Tag>::class.java).toList()
    }

    object NoopDeserializer : ResponseDeserializable<Unit> {
        override fun deserialize(content: String): Unit = Unit
    }
}