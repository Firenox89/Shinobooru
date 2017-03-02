package com.github.firenox89.shinobooru.ui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.support.v4.view.PagerAdapter
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.github.firenox89.shinobooru.model.Post
import com.github.firenox89.shinobooru.model.PostLoader
import com.ortiz.touch.TouchImageView
import fr.castorflex.android.verticalviewpager.VerticalViewPager
import org.jetbrains.anko.*
import org.jetbrains.anko.support.v4.drawerLayout

/**
 * Created by firenox on 2/26/17.
 */
class AlternatePostPagerActivity : Activity() {
    val TAG = "AlternatePostPagerActiv"
    lateinit private var postLoader: PostLoader
    lateinit var board: String
    lateinit var tags: String
    lateinit var drawerContent: AlternatePostPagerActivity.DetailsDrawer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //better fail here then later
        board = intent.getStringExtra("board") ?: throw IllegalArgumentException("Postloader can't be null")
        //TODO: check if there are post available for the tags, switching to an empty Activity is kinda stupid...
        tags = intent.getStringExtra("tags") ?: ""
        postLoader = PostLoader.getLoader(board, tags)

        val posi = intent.getIntExtra("posi", -1)
        if (posi == -1) throw IllegalArgumentException("Position must not be null")

        drawerLayout {
            fitsSystemWindows = true
            addView(VerticalViewPager(ctx).apply {
                id = View.generateViewId()
                adapter = PostPageAdapter()
                currentItem = posi
            })
            linearLayout {
                val display = windowManager.defaultDisplay
                val size = Point()
                display.getSize(size)
                lparams(width = size.x / 4 * 3, height = matchParent, gravity = Gravity.END)
                backgroundColor = Color.parseColor("#111111")
                drawerContent = DetailsDrawer(ctx, posi)
                addView(drawerContent)
            }
        }
    }

    inner class DetailsDrawer(ctx: Context, position: Int) : LinearLayout(ctx) {
        var view: View

        init {
            view = PostDetailsAnko<LinearLayout>(postLoader.getPostAt(position))
                    .createView(AnkoContext.create(ctx, this))
            addView(view)
        }

        fun changeView(post: Post) {
            removeView(view)
            ctx.runOnUiThread {
                view = PostDetailsAnko<LinearLayout>(post)
                        .createView(AnkoContext.create(ctx, this@DetailsDrawer))
                addView(view)
            }
        }
    }

    inner class PostPageAdapter : PagerAdapter() {

        init {
            //subscribe for new posts
            postLoader.getRangeChangeEventStream().subscribe {
                runOnUiThread {
                    notifyDataSetChanged()
                }
            }
        }

        override fun instantiateItem(container: ViewGroup?, position: Int): Any {
            //request new posts when less then 5 posts are left to load
            if (postLoader.getCount() - position > 5) postLoader.requestNextPosts()
            val imageview = TouchImageView(container?.context)
            container?.addView(imageview)

            setResult(position)
            val post: Post = PostLoader.getLoader(board, tags).getPostAt(position)

            post.loadSample {
                this@AlternatePostPagerActivity.runOnUiThread {
                    imageview.setImageBitmap(it)
                }
            }
            return imageview
        }

        override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any?) {
            super.setPrimaryItem(container, position, `object`)
            drawerContent.changeView(postLoader.getPostAt(position))
        }

        override fun destroyItem(container: ViewGroup?, position: Int, `object`: Any?) {
            container?.removeView(`object` as View?)
        }

        override fun isViewFromObject(view: View?, `object`: Any?): Boolean {
            return view == `object`
        }

        override fun getCount(): Int {
            return postLoader.getCount()
        }

    }
}