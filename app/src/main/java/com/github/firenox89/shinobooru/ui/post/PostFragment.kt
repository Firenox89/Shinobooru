package com.github.firenox89.shinobooru.ui.post

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * Contains the two child fragments.
 */
class PostFragment : Fragment() {
    val TAG = "PostFragment"
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val board = arguments!!.getString("board")
        val tags = arguments!!.getString("tags")
        val posi = arguments!!.getInt("posi")

        return ViewPager(context!!).apply {
            id = View.generateViewId()
            adapter = PostDetailsPagerAdapter(childFragmentManager, board, tags, posi, this.context)
        }
    }
}