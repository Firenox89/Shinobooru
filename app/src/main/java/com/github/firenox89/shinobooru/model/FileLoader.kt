package com.github.firenox89.shinobooru.model

class FileLoader: PostLoader("FileLoader", "") {
    val posts = FileManager.getAllPosts()

    override fun getPostAt(position: Int): Post? {
        return posts[position]
    }

    override fun requestNextPosts(quantity: Int) {
        //nothing to request in file mode
    }

    override fun getCount(): Int {
        return posts.size
    }

    override fun getPositionFor(post: Post): Int {
        return posts.indexOf(post)
    }

    override fun onRefresh(quantity: Int) {
        //no refresh either
    }
}