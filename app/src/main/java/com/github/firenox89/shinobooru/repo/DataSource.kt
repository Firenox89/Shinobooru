package com.github.firenox89.shinobooru.repo

import com.github.firenox89.shinobooru.repo.model.DownloadedPost
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

interface DataSource {
    fun getBoards(): List<String>
    fun getPostLoader(board: String, tags: String?): PostLoader
    fun getAllPosts(): Map<String, List<DownloadedPost>>
}

class DefaultDataSource : DataSource {
    private val loaderList = mutableListOf<PostLoader>().apply { add(FileLoader()) }

    val tmpBoards = listOf("yande.re", "konachan.com", "moe.booru.org", "danbooru.donmai.us", "gelbooru.com")
    /**
     * Returns a [PostLoader] instance for the given arguments.
     * Cache create instances and return them on the same arguments.
     * To obtain an instance of the [FileLoader] use 'FileLoader' as board name
     *
     * @param board that this loader should load from
     * @param tags this loader should add for requests
     * @return a cached or newly created instance of a [PostLoader]
     */
    override fun getPostLoader(board: String, tags: String?): PostLoader {
        var loader = loaderList.find { it.board == board && it.tags == tags }
        if (loader == null) {
            loader = RemotePostLoader(board, tags ?: "")
            loaderList.add(loader)
        }
        return loader
    }

    override fun getBoards(): List<String> = tmpBoards

    override fun getAllPosts(): Map<String, List<DownloadedPost>> = FileManager.boards


    /**
     * Reloads the posts for all stored loader instances
     */
    fun ratingChanged() {
        //TODO: set a flag for currently not used loader instead of reloading them all
        GlobalScope.launch {
            loaderList.forEach { it.onRefresh(-1) }
        }
    }
}