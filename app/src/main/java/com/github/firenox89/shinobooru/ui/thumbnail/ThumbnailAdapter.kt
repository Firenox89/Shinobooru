package com.github.firenox89.shinobooru.ui.thumbnail

import android.graphics.*
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.model.DownloadedPost
import com.github.firenox89.shinobooru.utility.PostLoader
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import org.jetbrains.anko.*

/**
 * [RecyclerView.Adapter] that provides the post images.
 */
class ThumbnailAdapter(var postLoader: PostLoader) : RecyclerView.Adapter<ThumbnailAdapter.PostViewHolder>() {

    val TAG = "ThumbnailAdapter"
    //emits click events for the clicked images
    val onImageClickStream = PublishSubject.create<Int>()

    private var postLoaderChangeSubscription = subscribeLoader()
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
     * Sets the given [PostLoader], removes the old post subscription and creates a new one.
     *
     * @param postLoader to switch to
     */
    fun changePostLoader(postLoader: PostLoader) {
        this.postLoader = postLoader
        postLoaderChangeSubscription.dispose()
        postLoaderChangeSubscription = subscribeLoader()
        notifyDataSetChanged()
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
            post.loadPreview {
                val img = it
                doAsync(Throwable::printStackTrace) {
                    uiThread {
                        holder.postImage.setImageBitmap(img)
                    }
                }
            }
        else
            post.loadSample {
                val img = it
                doAsync(Throwable::printStackTrace) {
                    uiThread {
                        holder.postImage.setImageBitmap(img)
                    }
                }
            }

//        holder.downloadedIcon.visibility = if (post is DownloadedPost) View.VISIBLE else View.INVISIBLE
//        holder.viewedIcon.visibility = if (post.wasViewed()) View.VISIBLE else View.INVISIBLE
        holder.itemView?.setOnClickListener { onImageClickStream.onNext(position) }
    }

    override fun getItemCount(): Int {
        return postLoader.getCount()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        return PostViewHolder(RelativeLayout(parent.context))
    }

    /**
     * A [RecyclerView.ViewHolder] containing the post image and to icons.
     */
    class PostViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(parent) {
        lateinit var postImage: ImageView
        lateinit var downloadedIcon: ImageView
        lateinit var viewedIcon: ImageView

        init {
            parent.addView(with(AnkoContext.create(parent.context, parent)) {
                relativeLayout {
                    lparams(width = matchParent, height = wrapContent)
                    imageView {
                        postImage = this
                        padding = 5
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        adjustViewBounds = true
                    }.lparams(width = matchParent, height = wrapContent)
                    linearLayout {
                        layoutParams =
                                RelativeLayout.LayoutParams(
                                        RelativeLayout.LayoutParams.MATCH_PARENT,
                                        RelativeLayout.LayoutParams.MATCH_PARENT)
                                        .apply {
                                            addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                                            addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                                        }
                        imageView {
                            downloadedIcon = this
                            imageBitmap = BitmapFactory.decodeResource(resources, R.drawable.cloud_download_2_32x32)
                            visibility = View.INVISIBLE
                            padding = 10
                        }
                        imageView {
                            viewedIcon = this
                            imageBitmap = BitmapFactory.decodeResource(resources, R.drawable.view_32x32)
                            visibility = View.INVISIBLE
                            padding = 10
                        }
                    }
                }
            })
        }
    }
}