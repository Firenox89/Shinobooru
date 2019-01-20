package com.github.firenox89.shinobooru.utility

import android.graphics.Bitmap
import com.github.firenox89.shinobooru.repo.model.DownloadedPost
import com.github.firenox89.shinobooru.repo.model.Post
import com.github.firenox89.shinobooru.repo.model.PostLoader
import com.github.firenox89.shinobooru.repo.model.Tag
import io.reactivex.Flowable
import io.reactivex.Single
import java.util.*

/**
 * Sub-classes the [PostLoader] to use the downloaded post images as a source for posts.
 * Does not refresh the post list when new images are downloaded.
 */
class FileLoader : PostLoader {
    override val board: String
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val tags: String
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun downloadPost(currentItem: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun loadPreview(post: Post): Single<Bitmap> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun loadSample(post: Post): Single<Bitmap> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTagList(post: Post): Single<List<Tag>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRangeChangeEventStream(): Flowable<Nothing> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

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

    private var posts = FileManager.getAllDownloadedPosts().sortedWith(newestDownloadedPostComparator)

    /**
     * Return a post from the postlist for the given number
     */
    override fun getPostAt(index: Int): Post {
        return posts[index]
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
        posts = FileManager.getAllDownloadedPosts().sortedWith(newestDownloadedPostComparator)
    }
}