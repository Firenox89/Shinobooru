package com.github.firenox89.shinobooru.ui

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import com.github.firenox89.shinobooru.model.DownloadedPost
import com.github.firenox89.shinobooru.model.Post
import com.github.firenox89.shinobooru.model.PostLoader

/**
 * Creates a [PostImageFragment] and a [PostDetailsFragment] from the given post.
 * @param fm the child fragment manager
 * @param post to create the fragments for
 */
class PostDetailsPagerAdapter(fm: FragmentManager, val board: String, val tags: String,
                              val posi: Int, val context: Context) : FragmentPagerAdapter(fm) {

    val post: Post = PostLoader.getLoader(board, tags).getPostAt(posi)!!

    /** mark the given post as viewed */
    init {
        PostLoader.addPostIdToViewedList(post.id)
    }

    /**
     * Position 0 returns the image fragment, 1 the details fragment.
     *
     * @param position of the fragment to return
     * @return the fragment for the postion
     */
    override fun getItem(position: Int): Fragment? {
        var fragment: Fragment
        if (position == 0) {
            fragment = PostImageFragment()
        } else {
            //TODO: intercept click events for this fragment
            fragment = PostDetailsFragment()
        }
        fragment.apply {
            arguments = Bundle().apply {
                putString("board", board)
                putString("tags", tags)
                putInt("posi", posi)
            }
        }
        return fragment
    }

    /**
     * If the post was create from a downloaded file there are no detail information yet,
     * so only the image fragment get created for it.
     */
    override fun getCount(): Int {
        return 2
    }
}