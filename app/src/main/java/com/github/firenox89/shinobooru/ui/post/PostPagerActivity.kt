package com.github.firenox89.shinobooru.ui.post

import android.os.Bundle
import android.support.v4.app.*
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.model.Post
import com.github.firenox89.shinobooru.utility.PostLoader
import com.github.salomonbrys.kodein.KodeinInjected
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import fr.castorflex.android.verticalviewpager.VerticalViewPager
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

/**
 * Creates a [VerticalViewPager] that starts on a given [Post] and load new posts from the given [PostLoader]
 */
class PostPagerActivity : FragmentActivity(), KodeinInjected {

    val TAG = "PostPagerActivity"
    override val injector = KodeinInjector()

    val width: Int by instance("width")
    val height: Int by instance("height")

    lateinit private var postLoader: PostLoader
    lateinit var board: String
    lateinit var tags: String

    //used for click events
    var x1 = 0F
    var x2 = 0F
    var y2 = 0F
    val SWIPE_DISTANCE = 1000
    val CLICK_DISTANCE = 5

    private val onPostSwitch = PublishSubject.create<Int>()
    private val clickEventStream = PublishSubject.create<MotionEvent>()

    /**
     * Creates a [VerticalViewPager] that starts on a given [Post] and load new posts from the given [PostLoader]
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        inject(appKodein())

        //better fail here then later
        board = intent.getStringExtra("board") ?: throw IllegalArgumentException("Postloader can't be null")
        //TODO: check if there are post available for the tags, switching to an empty Activity is kinda stupid...
        tags = intent.getStringExtra("tags") ?: ""
        postLoader = PostLoader.getLoader(board, tags)

        val posi = intent.getIntExtra("posi", -1)
        if (posi == -1) throw IllegalArgumentException("Position must not be null")

        val verticalPager = VerticalViewPager(this).apply {
            id = View.generateViewId()
            adapter = PostPagerAdapter(supportFragmentManager, postLoader, this@PostPagerActivity)
            currentItem = posi
        }

        onPostSwitch.subscribe { runOnUiThread { verticalPager.currentItem += it } }

        clickEventStream.buffer(100, TimeUnit.MILLISECONDS).forEach {
            when (it.size) {
                1 -> singleClick()
                2 -> doubleClick()
            }
        }

        setContentView(verticalPager)

        verticalPager.rootView.setBackgroundColor(resources.getColor(R.color.richtigesGrau))
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
        //this ist stupid while the download buttom is on the left side
//        else if (x2 < width / 10)
//            finish()
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

}
