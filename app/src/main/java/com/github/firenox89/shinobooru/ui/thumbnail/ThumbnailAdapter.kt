package com.github.firenox89.shinobooru.ui.thumbnail

import android.content.Context
import android.graphics.*
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.ext.defaultSchedulers
import com.github.firenox89.shinobooru.repo.model.DataSource
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import org.koin.android.ext.android.inject

/**
 * [RecyclerView.Adapter] that provides the post images.
 */
class ThumbnailAdapter(val context: Context, val board: String, val tags: String) : RecyclerView.Adapter<ThumbnailAdapter.PostViewHolder>() {

    val dataSource: DataSource by inject()
    val postLoader = dataSource.getPostLoader(board, tags)

    //emits click events for the clicked images
    val onImageClickStream = PublishSubject.create<Int>()

    var usePreview = true

    fun createPlaceholderBitmap(width: Int, height: Int): Bitmap {
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
    fun subscribeLoader(): Disposable {
        return postLoader.getRangeChangeEventStream().subscribe {
            val (posi, count) = it
            //if range starts with 0 send a dataChangedEvent instead of a rangeChangedEvent
            if (posi != 0) {
                notifyItemRangeChanged(posi, count)
            } else {
                notifyDataSetChanged()
            }
        }
    }

    /**
     * Fill the given [PostViewHolder] with the content from the [Post] at the given positon.
     * Also requests new posts if there a less than 5 left to load.
     */
    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postLoader.getPostAt(position)
        if (itemCount - position < 5) postLoader.requestNextPosts()

        holder.postImage.setImageBitmap(createPlaceholderBitmap(post.preview_width, post.preview_height))

        //if the recyclerView is set to one image per row use the sample image for quality reasons
        if (usePreview)
            postLoader.loadPreview(post)
                    .defaultSchedulers()
                    .subscribe { bitmap ->
                        holder.postImage.setImageBitmap(bitmap)
                    }
        else
            postLoader.loadSample(post)
                    .defaultSchedulers()
                    .subscribe { bitmap ->
                        holder.postImage.setImageBitmap(bitmap)
                    }

        holder.itemView.setOnClickListener { onImageClickStream.onNext(position) }
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
    }
}