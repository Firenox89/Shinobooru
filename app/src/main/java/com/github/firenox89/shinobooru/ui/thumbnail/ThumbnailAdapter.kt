package com.github.firenox89.shinobooru.ui.thumbnail

import android.graphics.*
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.repo.PostLoader
import com.github.firenox89.shinobooru.ui.showToast
import kotlinx.coroutines.*
import timber.log.Timber

class ThumbnailAdapter(val lifecycleScoop: CoroutineScope, val postLoader: PostLoader, private val postClickCallback: (Int) -> Unit) : RecyclerView.Adapter<ThumbnailAdapter.PostViewHolder>() {

    var usePreview = true

    private fun createPlaceholderBitmap(width: Int, height: Int): Bitmap {
        val rect = Rect(0, 0, width, height)
        val image = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        val color = Color.argb(255, 80, 80, 80)
        val paint = Paint()
        paint.color = color
        canvas.drawRect(rect, paint)
        return image
    }

    /**
     * Subscribe for newly loaded posts.
     */
    suspend fun subscribeLoader() {
        Timber.d("Subscribe to $postLoader")

        lifecycleScoop.launch {
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
        val post = postLoader.getPostAt(position)
        Timber.w("Bind post $post")

        holder.postImage.setImageBitmap(createPlaceholderBitmap(
                post.preview_width.coerceAtMost(250),
                post.preview_height.coerceAtMost(400)))

        lifecycleScoop.launch {
            withContext(Dispatchers.IO) {
                if (itemCount - position < 5) postLoader.requestNextPosts()

                //if the recyclerView is set to one image per row use the sample image for quality reasons
                if (usePreview) {
                    postLoader.loadPreview(post).fold({ bitmap ->
                        holder.updateImage(bitmap)
                    }, { error ->
                        Timber.e(error)
                        showToast(holder.context, "Loading failed $error")
                    })
                } else {
                    postLoader.loadSample(post).fold({ bitmap ->
                        holder.updateImage(bitmap)
                    }, { error ->
                        Timber.e(error)
                        showToast(holder.context, "Loading failed $error")
                    })
                }
            }
            holder.setListener { postClickCallback.invoke(position) }
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
        val context = parent.context

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