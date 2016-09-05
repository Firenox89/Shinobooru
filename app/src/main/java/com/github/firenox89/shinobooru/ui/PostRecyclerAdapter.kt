package com.github.firenox89.shinobooru.ui

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

class PostRecyclerAdapter() : RecyclerView.Adapter<PostRecyclerAdapter.PostViewHolder>() {

    private val onClickSubject = PublishSubject<Int>()
    var usePreview = true

    init {
        PostLoader.getRangeChangeEventStream().subscribe {
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
        val post = PostLoader.getPostAt(position)

        //if the recyclerView is set to one image per row use the sample image for quality reasons
        if (usePreview)
            post?.loadPreview { holder?.image?.setImageBitmap(it) }
        else
            post?.loadSample { holder?.image?.setImageBitmap(it) }

//        holder?.downloadedIcon?.visibility = if (post?.hasFile() ?: false) View.VISIBLE else View.INVISIBLE
//        holder?.viewedIcon?.visibility = if (post?.wasViewd() ?: false) View.VISIBLE else View.INVISIBLE

        holder?.itemView?.setOnClickListener { onClickSubject.onNext(position) }
    }

    fun getPositionClicks(): Observable<Int> {
        return onClickSubject.asObservable()
    }

    override fun getItemCount(): Int {
        return PostLoader.getCount()
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): PostViewHolder {
        val v = LayoutInflater.from(parent?.context).inflate(R.layout.view_holder_image, parent, false)
        return PostViewHolder(v)
    }

    class PostViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        //TODO: show already download
        //TODO: mark view posts
        val image: ImageView by bindView(R.id.postImage)
//        val downloadedIcon by bindView<ImageView>(R.id.downLoaded)
//        val viewedIcon by bindView<ImageView>(R.id.viewedIcon)
    }
}