package com.github.firenox89.shinobooru.ui

import android.graphics.*
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.model.PostLoader
import org.jetbrains.anko.*
import rx.Observable
import rx.Subscription
import rx.lang.kotlin.PublishSubject

class ThumbnailAdapter(var postLoader: PostLoader) : RecyclerView.Adapter<ThumbnailAdapter.PostViewHolder>() {

    private val onClickSubject = PublishSubject<Int>()
    var usePreview = true
    lateinit var subscription: Subscription

    val loadingBitmap: Bitmap by lazy {
        val rect = Rect(0, 0, 250, 400)
        val image = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        val color = Color.argb(255, 80, 80, 80)
        val paint = Paint()
        paint.color = color
        canvas.drawRect(rect, paint)
        image
    }

    init {
        subscribeLoader()
    }

    fun subscribeLoader() {
        subscription = postLoader.getRangeChangeEventStream().subscribe {
            //if range starts with 0 send a dataChangedEvent instead of a rangeChangedEvent
            if (it.first != 0) {
                val (posi, count) = it
                notifyItemRangeChanged(posi, count)
            } else {
                notifyDataSetChanged()
            }
        }
    }

    fun resetPostLoader(postLoader: PostLoader) {
        subscription.unsubscribe()
        this.postLoader = postLoader
        subscribeLoader()
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postLoader.getPostAt(position)
        if (postLoader.getCount() - position > 5) postLoader.requestNextPosts()

        holder.postImage.setImageBitmap(loadingBitmap)
        //if the recyclerView is set to one image per row use the sample image for quality reasons
        if (usePreview)
            post?.loadPreview { holder.postImage.setImageBitmap(it) }
        else
            post?.loadSample { holder.postImage.setImageBitmap(it) }

        holder.downloadedIcon.visibility = if (post?.hasFile() ?: false) View.VISIBLE else View.INVISIBLE
        holder.viewedIcon.visibility = if (post?.wasViewed() ?: false) View.VISIBLE else View.INVISIBLE
        holder.itemView?.setOnClickListener { onClickSubject.onNext(position) }
    }

    fun getPositionClicks(): Observable<Int> {
        return onClickSubject.asObservable()
    }

    override fun getItemCount(): Int {
        return postLoader.getCount()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        return PostViewHolder(RelativeLayout(parent.context))
    }

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
                        lparams(width = matchParent, height = wrapContent)
                    }
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
                            imageBitmap = BitmapFactory.decodeResource(resources, R.drawable.view_32x32)
                            padding = 10
                        }
                        imageView {
                            viewedIcon = this
                            imageBitmap = BitmapFactory.decodeResource(resources, R.drawable.cloud_download_2_32x32)
                            padding = 10
                        }
                    }
                }
            })
        }
    }
}