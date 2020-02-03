package com.github.firenox89.shinobooru.repo

import com.github.firenox89.shinobooru.repo.db.DBTag
import com.github.firenox89.shinobooru.repo.model.DownloadedPost
import com.github.firenox89.shinobooru.repo.model.Tag
import com.google.gson.Gson
import io.realm.Realm
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

interface DataSource {
    fun getBoards(): List<String>
    fun getAllPosts(): Map<String, List<DownloadedPost>>
    fun onRatingChanged()
    suspend fun getPostLoader(board: String, tags: String?): PostLoader
    suspend fun tagSearch(board: String, name: String): List<Tag>
    suspend fun loadTagColors(tags: List<Tag>): List<Tag>
}

class DefaultDataSource(val apiWrapper: ApiWrapper, val fileManager: FileManager, fileLoader: FileLoader) : DataSource {
    private val loaderList = mutableListOf<PostLoader>(fileLoader)

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
    override suspend fun getPostLoader(board: String, tags: String?): PostLoader {
        var loader = loaderList.find { it.board == board && it.tags == tags }
        if (loader == null) {
            loader = RemotePostLoader(board, tags ?: "", apiWrapper, fileManager)
            loader.requestNextPosts()
            loaderList.add(loader)
        }
        return loader
    }

    override fun getBoards(): List<String> = tmpBoards

    override fun getAllPosts(): Map<String, List<DownloadedPost>> = fileManager.boards

    override suspend fun tagSearch(board: String, name: String): List<Tag> {
        return Gson()
                .fromJson<Array<Tag>>(apiWrapper.requestTag(board, name), Array<Tag>::class.java)
                .toList()
                .also { saveTagsInDB(it, board) }
    }

    override suspend fun loadTagColors(tags: List<Tag>): List<Tag> =
            tags.map { tag ->
                loadTagFromDB(tag)?.toTag()
                        ?: loadTagFromAPI(tag)
                        ?: tag
            }

    private suspend fun loadTagFromAPI(tag: Tag): Tag? =
            tagSearch(tag.board, tag.name).firstOrNull { it.name == tag.name }

    /**
     * Reloads the posts for all stored loader instances
     */
    override fun onRatingChanged() {
        GlobalScope.launch {
            loaderList.forEach { it.onRefresh() }
        }
    }

    private fun saveTagsInDB(tags: List<Tag>, board: String) {
        tags.forEach { tag ->
            if (Realm.getDefaultInstance()
                            .where(DBTag::class.java)
                            .equalTo("id", tag.id)
                            .equalTo("board", board)
                            .findFirst() == null) {

                Realm.getDefaultInstance().executeTransaction { realm ->
                    realm.createObject(DBTag::class.java).apply {
                        this.id = tag.id
                        this.board = board
                        this.name = tag.name
                        this.type = tag.type
                    }
                }
            }
        }
    }

    private fun DBTag.toTag(): Tag =
            Tag(
                    id = this.id,
                    board = this.board,
                    name = this.name,
                    type = this.type
            )

    private fun loadTagFromDB(tag: Tag): DBTag? =
            Realm.getDefaultInstance()
                    .where(DBTag::class.java)
                    .equalTo("id", tag.id)
                    .and()
                    .equalTo("board", tag.board)
                    .findFirst()
}
