package com.github.firenox89.shinobooru.repo

import com.github.firenox89.shinobooru.repo.db.DBTag
import com.github.firenox89.shinobooru.repo.model.DownloadedPost
import com.github.firenox89.shinobooru.repo.model.Post
import com.github.firenox89.shinobooru.repo.model.Tag
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.Exception

interface DataSource {
    fun getBoards(): List<String>
    fun getAllPosts(): Map<String, List<DownloadedPost>>
    fun onRatingChanged()
    suspend fun getPostLoader(board: String, tags: String?): PostLoader
    suspend fun tagSearch(board: String, name: String): Result<List<Tag>, FuelError>
    suspend fun loadTagColors(tags: List<Tag>): List<Tag>
    suspend fun downloadPost(post: Post): Result<Unit, Exception>
    suspend fun deletePost(post: DownloadedPost): Result<Boolean, Exception>
}

class DefaultDataSource(val apiWrapper: ApiWrapper, val fileManager: FileManager, val storagePostLoader: StoragePostLoader) : DataSource {
    private val loaderList = mutableListOf<PostLoader>(storagePostLoader)

    val tmpBoards = listOf("yande.re", "konachan.com", "moe.booru.org", "danbooru.donmai.us", "gelbooru.com")
    /**
     * Returns a [PostLoader] instance for the given arguments.
     * Cache create instances and return them on the same arguments.
     * To obtain an instance of the [StoragePostLoader] use 'FileLoader' as board name
     *
     * @param board that this loader should load from
     * @param tags this loader should add for requests
     * @return a cached or newly created instance of a [PostLoader]
     */
    override suspend fun getPostLoader(board: String, tags: String?): PostLoader = withContext(Dispatchers.IO) {
        loaderList.find { it.board == board && it.tags == tags }
                ?: (RemotePostLoader(board, tags ?: "", apiWrapper, fileManager).apply {
                    requestNextPosts()
                    loaderList.add(this)
                })
    }

    override fun getBoards(): List<String> = tmpBoards

    override fun getAllPosts(): Map<String, List<DownloadedPost>> = fileManager.boards

    override suspend fun tagSearch(board: String, name: String): Result<List<Tag>, FuelError> =
            apiWrapper.requestTag(board, name).map { tags ->
                saveTagsInDB(tags, board)
                tags
            }


    override suspend fun loadTagColors(tags: List<Tag>): List<Tag> =
            tags.map { tag ->
                loadTagFromDB(tag)?.toTag()
                        ?: loadTagFromAPI(tag).get()
                        ?: tag
            }

    private suspend fun loadTagFromAPI(tag: Tag): Result<Tag, Exception> =
            tagSearch(tag.board, tag.name).map { list ->
                list.first { it.name == tag.name }
            }

    override suspend fun downloadPost(post: Post): Result<Unit, Exception> {
        Timber.i("Download Post $post")

        return fileManager.getDownloadDestinationFor(post).flatMap { destination ->
            apiWrapper.downloadPost(post, destination).also {
                if (it is Result.Success) {
                    fileManager.addDownloadedPost(post, destination).also {
                        storagePostLoader.onRefresh()
                    }
                }
            }
        }
    }

    override suspend fun deletePost(post: DownloadedPost): Result<Boolean, Exception> {
        Timber.i("Delete Post $post")

        return fileManager.deleteDownloadedPost(post).also {
            storagePostLoader.onRefresh()
        }
    }

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
            } else {
                Timber.w("Tag was already in DB $tag")
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
                    .equalTo("name", tag.name)
                    .equalTo("board", tag.board)
                    .findFirst()
}
