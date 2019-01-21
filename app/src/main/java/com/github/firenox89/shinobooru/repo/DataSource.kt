package com.github.firenox89.shinobooru.repo

interface DataSource {
    fun getPostLoader(board: String, tags: String?): PostLoader
}

class DefaultDataSource : DataSource {
    private val loaderList = mutableListOf<PostLoader>().apply { add(FileLoader()) }

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

    /**
     * Reloads the posts for all stored loader instances
     */
    fun ratingChanged() {
        //TODO: set a flag for currently not used loader instead of reloading them all
        loaderList.forEach { it.onRefresh(-1) }
    }
}