package com.github.firenox89.shinobooru.ui.post

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.github.firenox89.shinobooru.model.Post
import com.github.firenox89.shinobooru.utility.PostLoader
import com.ortiz.touch.TouchImageView
import org.jetbrains.anko.support.v4.onUiThread

/**
 * Get the post from the bundle argument and create a [ImageView] with sample image.
 */
class PostImageFragment : Fragment() {
    val TAG = "PostImageFragment"
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val board = arguments!!.getString("board")
        val tags = arguments!!.getString("tags")
        val posi = arguments!!.getInt("posi")
        val post: Post = PostLoader.getLoader(board, tags).getPostAt(posi)

        val imageview = TouchImageView(context)
        //display preview image first for faster response
        post.loadPreview {
            if (activity != null) {
                onUiThread {
                    imageview.setImageBitmap(it)
                }
            } else {
                it?.recycle()
            }
        }
        //TODO check for the unlikely case that sample loaded fast then preview and preview overrides sample
        post.loadSample {
            if (activity != null) {
                onUiThread {
                    imageview.setImageBitmap(it)
                }
            } else {
                it?.recycle()
            }
        }
        return imageview
    }
}