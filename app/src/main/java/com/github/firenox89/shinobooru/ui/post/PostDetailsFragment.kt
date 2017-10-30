package com.github.firenox89.shinobooru.ui.post

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.firenox89.shinobooru.model.Post
import com.github.firenox89.shinobooru.utility.PostLoader
import org.jetbrains.anko.AnkoContext

/**
 * Get the post from the bundle arguments and use [PostDetailsAnko] to build the view.
 */
class PostDetailsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val board = arguments.getString("board")
        val tags = arguments.getString("tags")
        val posi = arguments.getInt("posi")
        val post: Post = PostLoader.getLoader(board, tags).getPostAt(posi)

        return PostDetailsAnko(post).createView(AnkoContext.create(context, this))
    }
}