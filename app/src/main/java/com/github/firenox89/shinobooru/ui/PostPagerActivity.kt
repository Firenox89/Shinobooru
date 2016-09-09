package com.github.firenox89.shinobooru.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.support.v4.app.*
import android.support.v4.view.ViewPager
import android.support.v7.app.NotificationCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.model.Post
import com.github.firenox89.shinobooru.model.PostLoader
import com.github.salomonbrys.kodein.KodeinInjected
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.ortiz.touch.TouchImageView
import rx.lang.kotlin.PublishSubject
import java.util.concurrent.TimeUnit

class PostPagerActivity : FragmentActivity(), KodeinInjected {

    override val injector = KodeinInjector()

    val width: Int by instance("width")
    val height: Int by instance("height")

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

        val post = intent.getSerializableExtra(resources.getString(R.string.post_class)) as Post

        val verticalPager = VerticalViewPager(this)
        //TODO: use proper id
        verticalPager.id = 0x12345678
        verticalPager.adapter = PostPagerAdapter(supportFragmentManager)
        verticalPager.currentItem = PostLoader.getPositionFor(post)

        //TODO: record time intervals and suggest "slideshow mode"
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
        if (y2 < height / 3)
            onPostSwitch.onNext(1)
        else if (y2 > height - height / 3)
            onPostSwitch.onNext(-1)
        else if (x2 < width / 10)
            finish()
        //TODO: return current post for scrolling
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
        super.dispatchTouchEvent(event)
        return false
    }

    inner class PostPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        init {
            PostLoader.getRangeChangeEventStream().subscribe { notifyDataSetChanged() }
        }

        override fun getItem(position: Int): Fragment? {

            val post = PostLoader.getPostAt(position)
            if (PostLoader.getCount() - position > 5) PostLoader.requestNextPosts()

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
            return PostLoader.getCount()
        }
    }


    class PostFragment() : Fragment() {

        override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val post = arguments.getSerializable(resources.getString(R.string.post_class)) as Post

            val rootView: View? = inflater?.inflate(R.layout.fragment_post, container, false)

            val viewPager = rootView?.findViewById(R.id.post_details_pager) as ViewPager
            viewPager.adapter = PostDetailsPagerAdapter(childFragmentManager, post, this.context)

            return rootView
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

            val rootView: View? = inflater?.inflate(R.layout.fragment_post_image, container, false)

            val imageview = rootView?.findViewById(R.id.postImage) as TouchImageView
            //TODO: check sizes
            post.loadSample { imageview.setImageBitmap(it) }
            return rootView
        }
    }

    class PostDetailsFragment : Fragment() {

        override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val post = arguments.getSerializable(resources.getString(R.string.post_class)) as Post

            val rootView: View? = inflater?.inflate(R.layout.fragment_post_details, container, false)

            (rootView?.findViewById(R.id.fileSize) as TextView).text = "File size = ${humanizeSize(post.file_size)}"
            (rootView?.findViewById(R.id.fileDimension) as TextView).text = "File dimension = ${post.width}x${post.height}"
//        (rootView?.findViewById(R.id.fileURL) as TextView).text = "File URL = ${post.file_url}"
            (rootView?.findViewById(R.id.fileDownloadButton) as ImageButton).setOnClickListener {
                post.downloadFile()
                rootView?.context?.let { sendNotify(it, post.file_url) }
            }

            //TODO: dont show if not there
            (rootView?.findViewById(R.id.jpegSize) as TextView).text = "Jpeg size = ${humanizeSize(post.jpeg_file_size)}"
            (rootView?.findViewById(R.id.jpegDimension) as TextView).text = "Jpeg dimension = ${post.jpeg_width}x${post.jpeg_height}"
//        (rootView?.findViewById(R.id.jpegURL) as TextView).text = "Jpeg URL = ${post.jpeg_url}"
            (rootView?.findViewById(R.id.jpegDownloadButton) as ImageButton).setOnClickListener {
                post.downloadJpeg()
                rootView?.context?.let { sendNotify(it, post.jpeg_url) }
            }

            (rootView?.findViewById(R.id.sampleSize) as TextView).text = "Sample size = ${humanizeSize(post.sample_file_size)}"
            (rootView?.findViewById(R.id.sampleDimension) as TextView).text = "Sample dimension = ${post.sample_width}x${post.sample_height}"
//        (rootView?.findViewById(R.id.sampleURL) as TextView).text = "Sample URL = ${post.sample_url}"
            (rootView?.findViewById(R.id.sampleDownloadButton) as ImageButton).setOnClickListener {
                post.downloadSample()
                rootView?.context?.let { sendNotify(it, post.sample_url) }
            }

            return rootView
        }

        fun sendNotify(context: Context, url: String) {
            val text = "Download $url"
            val duration = Toast.LENGTH_SHORT

            val toast = Toast.makeText(context, text, duration)
            toast.show()

            val mBuilder = NotificationCompat.Builder(context).
                    setSmallIcon(R.drawable.cloud_download_2_32x32).
                    setContentTitle("Download of").
                    setContentText(url)
            val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mNotificationManager.notify(1, mBuilder.build())
        }

        private fun humanizeSize(size: Int): String {
            //TODO: fractional digits
            if (size > 1024 * 1024)
                return "${(size.toDouble() / (1024 * 1024)).format(2)} M"
            if (size > 1024)
                return "${(size.toDouble() / 1024).format(2)} K"
            return "$size"
        }

        fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)
    }
}
