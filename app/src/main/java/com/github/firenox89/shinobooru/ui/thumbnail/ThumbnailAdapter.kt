package com.github.firenox89.shinobooru.ui.thumbnail

import android.graphics.*
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.repo.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * [RecyclerView.Adapter] that provides the post images.
 */
class ThumbnailAdapter(dataSource: DataSource, val board: String, val tags: String) : RecyclerView.Adapter<ThumbnailAdapter.PostViewHolder>() {

    val postLoader = dataSource.getPostLoader(board, tags)

    //emits click events for the clicked images
    val onImageClickStream = Channel<Int>()

    var usePreview = true

    private suspend fun createPlaceholderBitmap(width: Int, height: Int): Bitmap = withContext(Dispatchers.IO) {
        val rect = Rect(0, 0, width, height)
        val image = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        val color = Color.argb(255, 80, 80, 80)
        val paint = Paint()
        paint.color = color
        canvas.drawRect(rect, paint)
        image
    }

    /**
     * Subscribe for newly loaded posts.
     */
    fun subscribeLoader() {
        GlobalScope.launch {
            for (update in postLoader.getRangeChangeEventStream()) {
                Timber.d("post loader update $update")
                notify(update)
            }
        }
    }

    suspend fun notify(update: Pair<Int, Int>) = withContext(Dispatchers.Main) {
        val (posi, count) = update
        //if range starts with 0 send a dataChangedEvent instead of a rangeChangedEvent
        if (posi != 0) {
            notifyItemRangeChanged(posi, count)
        } else {
            notifyDataSetChanged()
        }
    }

    /**
     * Fill the given [PostViewHolder] with the content from the [Post] at the given positon.
     * Also requests new posts if there a less than 5 left to load.
     */
    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        GlobalScope.launch {
            val post = postLoader.getPostAt(position)
            holder.updateImage(createPlaceholderBitmap(post.preview_width, post.preview_height))

            if (itemCount - position < 5) postLoader.requestNextPosts()

            //if the recyclerView is set to one image per row use the sample image for quality reasons
            if (usePreview)
                postLoader.loadPreview(post)
                        .let { bitmap ->
                            holder.updateImage(bitmap)
                        }
            else
                postLoader.loadSample(post)
                        .let { bitmap ->
                            holder.updateImage(bitmap)
                        }

            holder.setListener { onImageClickStream.offer(position) }
        }
    }

    override fun getItemCount(): Int {
        return postLoader.getCount()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val infalter: LayoutInflater = LayoutInflater.from(parent.context)
        val view = infalter.inflate(R.layout.view_holder_thumbnail, parent, false)
        return PostViewHolder(view)
    }

    /**
     * A [RecyclerView.ViewHolder] containing the post image and to icons.
     */
    class PostViewHolder(parent: View) : RecyclerView.ViewHolder(parent) {
        var postImage: ImageView = parent.findViewById(R.id.thumbnailView)

        suspend fun updateImage(image: Bitmap) = withContext(Dispatchers.Main) {
            postImage.setImageBitmap(image)
        }

        suspend fun setListener(listener: () -> Unit) = withContext(Dispatchers.Main) {
            postImage.setOnClickListener {
                listener.invoke()
            }
        }
    }
}