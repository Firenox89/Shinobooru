package com.github.firenox89.shinobooru.ui.post

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import com.github.firenox89.shinobooru.repo.PostLoader

/**
 * Requests post from the [PostLoader] and create fragments out of it.
 */
class PostPagerAdapter(fm: FragmentManager, val postLoader: PostLoader, val activity: Activity) : FragmentStatePagerAdapter(fm) {
    init {
        //subscribe for new posts
        postLoader.getRangeChangeEventStream().subscribe {
            activity.runOnUiThread {
                notifyDataSetChanged()
            }
        }
    }

    /**
     * Returns the [PostFragment] for the given position.
     * Also sets the activity result to the current position.
     */
    override fun getItem(position: Int): Fragment? {
        //request new posts when less then 5 posts are left to load
        if (postLoader.getCount() - position > 5) postLoader.requestNextPosts()

        //set activity result to current post for scrolling in thumbnail view
        //nothing to fail here so result is always ok
        activity.setResult(Activity.RESULT_OK, Intent().apply {
            putExtra("position", position)
        })

        return PostFragment().apply {
            arguments = Bundle().apply {
                putString("board", postLoader.board)
                putString("tags", postLoader.tags)
                putInt("posi", position)
            }
        }
    }

    override fun getCount(): Int {
        return postLoader.getCount()
    }
}