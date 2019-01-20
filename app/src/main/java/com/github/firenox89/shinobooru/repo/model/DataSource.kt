package com.github.firenox89.shinobooru.repo.model

import com.github.firenox89.shinobooru.utility.RemotePostLoader

interface DataSource {
    fun getPostLoader(board: String, tags: String?): PostLoader
}

class DefaultDataSource: DataSource {
    override fun getPostLoader(board: String, tags: String?): PostLoader {
        return RemotePostLoader(board, tags ?: "")
    }
}