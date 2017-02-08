package com.github.firenox89.shinobooru.model

import java.util.*

/**
 * Sub-classes the [PostLoader] to use the downloaded post images as a source for posts.
 * Does not refresh the post list when new images are downloaded.
 */
internal class FileLoader : PostLoader("FileLoader", "") {
    private val newestDownloadedPostComparator = Comparator<DownloadedPost> { post1, post2 ->
        val date1 = post1.file.lastModified()
        val date2 = post2.file.lastModified()
        var result = 0
        if (date1 < date2)
            result = 1
        if (date1 > date2)
            result = -1
        result
    }

    private val posts = FileManager.getAllDownloadedPosts().sortedWith(newestDownloadedPostComparator)

    /**
     * Return a post from the postlist for the given number
     */
    override fun getPostAt(position: Int): Post? {
        return posts[position]
    }

    /** Does nothing */
    override fun requestNextPosts(quantity: Int) {
        //nothing to request in file mode
    }

    /**
     * Returns the size  of the postlist.
     */
    override fun getCount(): Int {
        return posts.size
    }

    /**
     * Return the index for a post in the postlist.
     */
    override fun getIndexOf(post: Post): Int {
        return posts.indexOf(post)
    }

    /** Does nothing */
    override fun onRefresh(quantity: Int) {
        //no refresh either
    }
}