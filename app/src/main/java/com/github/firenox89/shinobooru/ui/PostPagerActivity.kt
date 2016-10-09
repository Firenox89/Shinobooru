package com.github.firenox89.shinobooru.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.*
import android.support.v4.view.ViewPager
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.model.Post
import com.github.firenox89.shinobooru.model.PostLoader
import com.github.salomonbrys.kodein.KodeinInjected
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.ortiz.touch.TouchImageView
import org.jetbrains.anko.AnkoContext
import rx.lang.kotlin.PublishSubject
import java.util.concurrent.TimeUnit

class PostPagerActivity : FragmentActivity(), KodeinInjected {

    override val injector = KodeinInjector()

    val width: Int by instance("width")
    val height: Int by instance("height")

    lateinit private var postLoader: PostLoader

    var x1: Float = 0F
    var x2: Float = 0F
    var y2 = 0F
    val SWIPE_DISTANCE = 1000
    val CLICK_DISTANCE = 5

    private val onPostSwitch = PublishSubject<Int>()
    private val clickEventStream = PublishSubject<MotionEvent>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        inject(appKodein())

        val board = intent.getStringExtra("board") ?: ""
        val tags = intent.getStringExtra("tags") ?: ""
        postLoader = PostLoader.getLoader(board, tags)

        val post = intent.getSerializableExtra(resources.getString(R.string.post_class)) as Post

        val verticalPager = VerticalViewPager(this)
        //TODO: use proper id
        verticalPager.id = 0x12345678
        verticalPager.adapter = PostPagerAdapter(supportFragmentManager)
        verticalPager.currentItem = postLoader.getPositionFor(post)

        onPostSwitch.subscribe { runOnUiThread { verticalPager.currentItem += it } }

        clickEventStream.buffer(100, TimeUnit.MILLISECONDS).forEach {
            when (it.size) {
                1 -> singleClick()
                2 -> doubleClick()
            }
        }

        setContentView(verticalPager)
    }

    fun singleClick() {
        if (y2 < height / 10)
            onPostSwitch.onNext(-1)
        else if (y2 > height - height / 10)
            onPostSwitch.onNext(1)
        else if (x2 < width / 10)
            finish()
    }

    fun doubleClick() {
        Log.e("double", "click")
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> x1 = event?.x as Float
            MotionEvent.ACTION_UP -> {
                x2 = event?.x as Float
                y2 = event?.y as Float

                val deltaX = x2 - x1
                if (deltaX > SWIPE_DISTANCE) {
                }
                if (Math.abs(deltaX) < CLICK_DISTANCE) {
                    clickEventStream.onNext(event)
                }
            }
        }

        return super.dispatchTouchEvent(event)
    }

    inner class PostPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        init {
            postLoader.getRangeChangeEventStream().subscribe { notifyDataSetChanged() }
        }

        override fun getItem(position: Int): Fragment? {

            val post = postLoader.getPostAt(position)
            if (postLoader.getCount() - position > 5) postLoader.requestNextPosts()

            //set activity result to current post for scrolling in thumbnail view
            val intent = Intent()
            intent.putExtra(resources.getString(R.string.post_class), post)
            setResult(1, intent)

            val fragment = PostFragment()
            val args = Bundle()
            args.putSerializable(resources.getString(R.string.post_class), post)
            fragment.arguments = args

            return fragment
        }

        override fun getCount(): Int {
            return postLoader.getCount()
        }
    }


    class PostFragment() : Fragment() {

        override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val post = arguments.getSerializable(resources.getString(R.string.post_class)) as Post

            val viewPager = ViewPager(context)
            viewPager.id = 987654321
            viewPager.adapter = PostDetailsPagerAdapter(childFragmentManager, post, this.context)

            return viewPager
        }
    }

    class PostDetailsPagerAdapter(fm: FragmentManager, val post: Post, val context: Context) : FragmentPagerAdapter(fm) {

        init {
            PostLoader.addPostIdToViewedList(post.id)
        }
        //TODO: intercept click events
        override fun getItem(position: Int): Fragment? {
            if (position == 0) {
                val fragment = PostImageFragment()
                val args = Bundle()
                args.putSerializable(context.getString(R.string.post_class), post)
                fragment.arguments = args
                return fragment
            } else {
                val fragment = PostDetailsFragment()
                val args = Bundle()
                args.putSerializable(context.getString(R.string.post_class), post)
                fragment.arguments = args
                return fragment
            }
        }

        override fun getCount(): Int {
            return if (post.file != null) 1 else 2
        }
    }

    class PostImageFragment : Fragment() {

        override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val post = arguments.getSerializable(resources.getString(R.string.post_class)) as Post

            val imageview = TouchImageView(context)
            post.loadSample { imageview.setImageBitmap(it) }
            return imageview
        }
    }

    class PostDetailsFragment : Fragment() {

        override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val post = arguments.getSerializable(resources.getString(R.string.post_class)) as Post

            return PostDetailsAnko<Fragment>(post).createView(AnkoContext.create(context, this))
        }

    }
}
