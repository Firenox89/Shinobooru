package com.github.firenox89.shinobooru.ui

import android.graphics.*
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.model.PostLoader
import rx.Observable
import rx.lang.kotlin.PublishSubject
import kotterknife.bindView

class ThumbnailAdapter(val postLoader: PostLoader) : RecyclerView.Adapter<ThumbnailAdapter.PostViewHolder>() {

    private val onClickSubject = PublishSubject<Int>()
    var usePreview = true

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
        postLoader.getRangeChangeEventStream().subscribe {
            //if range starts with 0 send a dataChangedEvent instead of a rangeChangedEvent
            if (it.first != 0) {
                val (posi, count) = it
                notifyItemRangeChanged(posi, count)
            } else {
                notifyDataSetChanged()
            }
        }
    }

    override fun onBindViewHolder(holder: PostViewHolder?, position: Int) {
        val post = postLoader.getPostAt(position)
        if (postLoader.getCount() - position > 5) postLoader.requestNextPosts()

        holder?.image?.setImageBitmap(loadingBitmap)
        //if the recyclerView is set to one image per row use the sample image for quality reasons
        if (usePreview)
            post?.loadPreview { holder?.image?.setImageBitmap(it) }
        else
            post?.loadSample { holder?.image?.setImageBitmap(it) }

        holder?.downloadedIcon?.visibility = if (post?.hasFile() ?: false) View.VISIBLE else View.INVISIBLE
        holder?.viewedIcon?.visibility = if (post?.wasViewd() ?: false) View.VISIBLE else View.INVISIBLE
        holder?.itemView?.setOnClickListener { onClickSubject.onNext(position) }
    }

    fun getPositionClicks(): Observable<Int> {
        return onClickSubject.asObservable()
    }

    override fun getItemCount(): Int {
        return postLoader.getCount()
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): PostViewHolder {
        val v = LayoutInflater.from(parent?.context).inflate(R.layout.view_holder_image, parent, false)
        return PostViewHolder(v)
    }

    class PostViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        //TODO: show already download
        //TODO: mark view posts
        val image: ImageView by bindView(R.id.postImage)
        val downloadedIcon by bindView<ImageView>(R.id.downLoaded)
        val viewedIcon by bindView<ImageView>(R.id.viewedIcon)
    }
}