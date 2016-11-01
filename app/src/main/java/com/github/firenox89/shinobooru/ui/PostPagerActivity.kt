package com.github.firenox89.shinobooru.ui

import android.app.Activity
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
import com.github.firenox89.shinobooru.app.Shinobooru
import com.github.firenox89.shinobooru.model.Post
import com.github.firenox89.shinobooru.model.PostLoader
import com.github.salomonbrys.kodein.KodeinInjected
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.ortiz.touch.TouchImageView
import fr.castorflex.android.verticalviewpager.VerticalViewPager
import org.jetbrains.anko.AnkoContext
import rx.lang.kotlin.PublishSubject
import java.util.concurrent.TimeUnit

/**
 * Creates a [VerticalViewPager] that starts on a given [Post] and load new posts from the given [PostLoader]
 */
class PostPagerActivity : FragmentActivity(), KodeinInjected {

    override val injector = KodeinInjector()

    val width: Int by instance("width")
    val height: Int by instance("height")

    lateinit private var postLoader: PostLoader

    //used for click events
    var x1 = 0F
    var x2 = 0F
    var y2 = 0F
    val SWIPE_DISTANCE = 1000
    val CLICK_DISTANCE = 5

    private val onPostSwitch = PublishSubject<Int>()
    private val clickEventStream = PublishSubject<MotionEvent>()

    /**
     * Creates a [VerticalViewPager] that starts on a given [Post] and load new posts from the given [PostLoader]
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        inject(appKodein())

        //better fail here then later
        val board = intent.getStringExtra("board") ?: throw IllegalArgumentException("Postloader can't be null")
        //TODO: check if there are post available for the tags, switching to an empty Activity is kinda stupid...
        val tags = intent.getStringExtra("tags") ?: ""
        postLoader = PostLoader.getLoader(board, tags)

        val post = intent.getSerializableExtra(resources.getString(R.string.post_class)) as Post


        val verticalPager = VerticalViewPager(this).apply {
            //TODO: use proper id
            id = 0x12345678
            adapter = PostPagerAdapter(supportFragmentManager)
            currentItem = postLoader.getIndexOf(post)
        }

        onPostSwitch.subscribe { runOnUiThread { verticalPager.currentItem += it } }

        clickEventStream.buffer(100, TimeUnit.MILLISECONDS).forEach {
            when (it.size) {
                1 -> singleClick()
                2 -> doubleClick()
            }
        }

        setContentView(verticalPager)
    }

    /**
     * Get the position of the click event and do something dependent on the position.
     * Clicks on the first fifth of the screen will go on post back,
     * clicks on the last fifth of the screen will go on to the next post,
     * clicks on the left side will finish the activity.
     */
    fun singleClick() {
        if (y2 < height / 20)
            onPostSwitch.onNext(-1)
        else if (y2 > height - height / 20)
            onPostSwitch.onNext(1)
        else if (x2 < width / 10)
            finish()
    }

    /**
     * Just log the event, could be remove since double clicks are already used for zooming the image.
     */
    fun doubleClick() {
        Log.e("double", "click")
    }

    /**
     * Create click events but does not consume the [MotionEvent]
     */
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

    /**
     * Requests post from the [PostLoader] and create fragments out of it.
     */
    inner class PostPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        init {
            //subscribe for new posts
            postLoader.getRangeChangeEventStream().subscribe {
                runOnUiThread {
                    notifyDataSetChanged()
                }
            }
        }

        /**
         * Returns the [PostFragment] for the given position.
         * Also sets the activity result to the current position.
         */
        override fun getItem(position: Int): Fragment? {
            val post = postLoader.getPostAt(position)
            //request new posts when less then 5 posts are left to load
            if (postLoader.getCount() - position > 5) postLoader.requestNextPosts()

            //set activity result to current post for scrolling in thumbnail view
            //nothing to fail here so result is always ok
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra("position", position)
            })

            return PostFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(Shinobooru.appContext.getString(R.string.post_class), post)
                }
            }
        }

        override fun getCount(): Int {
            return postLoader.getCount()
        }
    }

    /**
     * Contains the two child fragments.
     */
    class PostFragment() : Fragment() {
        override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val post = arguments.getSerializable(resources.getString(R.string.post_class)) as Post

            return ViewPager(context).apply {
                //TODO: use proper id
                id = 987654321
                adapter = PostDetailsPagerAdapter(childFragmentManager, post, this.context)
            }
        }
    }

    /**
     * Creates a [PostImageFragment] and a [PostDetailsFragment] from the given post.
     * @param fm the child fragment manager
     * @param post to create the fragments for
     */
    class PostDetailsPagerAdapter(fm: FragmentManager, val post: Post, val context: Context) : FragmentPagerAdapter(fm) {

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
                    putSerializable(Shinobooru.appContext.getString(R.string.post_class), post)
                }
            }
            return fragment
        }

        /**
         * If the post was create from a downloaded file there are no detail information yet,
         * so only the image fragment get created for it.
         */
        override fun getCount(): Int {
            return if (post.file != null) 1 else 2
        }
    }

    /**
     * Get the post from the bundle argument and create a [TouchImageView] with sample image.
     */
    class PostImageFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val post = arguments.getSerializable(resources.getString(R.string.post_class)) as Post

            val imageview = TouchImageView(context)
            post.loadSample { imageview.setImageBitmap(it) }
            return imageview
        }
    }

    /**
     * Get the post from the bundle arguments and use [PostDetailsAnko] to build the view.
     */
    class PostDetailsFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val post = arguments.getSerializable(resources.getString(R.string.post_class)) as Post

            return PostDetailsAnko<Fragment>(post).createView(AnkoContext.create(context, this))
        }

    }
}
