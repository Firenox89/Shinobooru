package com.github.firenox89.shinobooru.repo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.view.WindowManager
import com.github.firenox89.shinobooru.app.Shinobooru
import com.github.firenox89.shinobooru.repo.model.DownloadedPost
import com.github.firenox89.shinobooru.repo.model.Post
import com.github.firenox89.shinobooru.repo.model.Tag
import com.github.firenox89.shinobooru.utility.Constants
import com.github.firenox89.shinobooru.utility.UI.loadSubsampledImage
import kotlinx.coroutines.channels.Channel

/**
 * Sub-classes the [PostLoader] to use the downloaded post images as a source for posts.
 * Does not refresh the post list when new images are downloaded.
 */
class FileLoader : PostLoader {
    override val board: String
        get() = Constants.FILE_LOADER_NAME
    override val tags: String
        get() = ""

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

    override fun downloadPost(currentItem: Int) {
        throw IllegalStateException("Post ")
    }

    override suspend fun loadPreview(post: Post): Bitmap = loadSubsampledImage((post as DownloadedPost).file, 250, 400)


    override suspend fun loadSample(post: Post): Bitmap {
        val wm = Shinobooru.appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()
        display.getSize(size)
        //sample huge images=
        return loadSubsampledImage((post as DownloadedPost).file, size.x, size.y)
    }

    override suspend fun getTagList(post: Post): List<Tag> =
            post.tags.split(" ").map { Tag(it, post.getBoard()) }


    override suspend fun getRangeChangeEventStream(): Channel<Pair<Int, Int>> =
        Channel<Pair<Int, Int>>().apply{ send(Pair(0, posts.size))}

    /**
     * Return a post from the postlist for the given number
     */
    override fun getPostAt(index: Int): Post {
        return posts[index]
    }

    /** Does nothing */
    override suspend fun requestNextPosts(quantity: Int) {
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
    override suspend fun onRefresh(quantity: Int) {
        posts = FileManager.getAllDownloadedPosts().sortedWith(newestDownloadedPostComparator)
    }

}