package com.github.firenox89.shinobooru.ui.post

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.ListAdapter
import android.widget.TextView
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.repo.DataSource
import com.github.firenox89.shinobooru.repo.model.Post
import com.github.firenox89.shinobooru.repo.model.Tag
import com.github.firenox89.shinobooru.ui.showToast
import com.github.firenox89.shinobooru.ui.thumbnail.ThumbnailActivity
import com.github.firenox89.shinobooru.utility.Constants.BOARD_INTENT_KEY
import com.github.firenox89.shinobooru.utility.Constants.TAGS_INTENT_KEY
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber
import kotlin.math.roundToInt

class PostFragment : androidx.fragment.app.Fragment() {
    private val dataSource: DataSource by inject()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_post, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val board = arguments?.getString("board")
        val tags = arguments?.getString("tags")
        val postIndex = arguments?.getInt("posi")

        if (board == null || postIndex == null) {
            //TODO don't just show an empty view
            return
        }
        val imageView = view.findViewById<ImageView>(R.id.postimage)
        val authorText = view.findViewById<TextView>(R.id.authorText)
        val sourceText = view.findViewById<TextView>(R.id.sourceText)
        val postText = view.findViewById<TextView>(R.id.postInfoText)
        val tagListView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.tagList)

        lifecycleScope.launch {
            val postLoader = dataSource.getPostLoader(board, tags)
            val post: Post = postLoader.getPostAt(postIndex)

            Timber.d("Show post $postIndex")

            tagListView.adapter = TagListAdapter(lifecycleScope, post)
            tagListView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(context, 2)

            authorText.text = String.format(resources.getText(R.string.author_s).toString(), post.author)
            sourceText.text = String.format(resources.getText(R.string.source_s).toString(), post.source)
            postText.text = String.format(resources.getText(R.string.board_s_id_s).toString(), post.getBoard(), post.id)

            //display preview image first for faster response
            val loadPreviewJob = launch {
                postLoader.loadPreview(post).fold({ bitmap ->
                    withContext(Dispatchers.Main) { imageView.setImageBitmap(bitmap) }
                }, { error ->
                    Timber.e(error)
                    context?.run { showToast(this, "Loading failed $error") }
                })
            }
            postLoader.loadSample(post).fold({ bitmap ->
                loadPreviewJob.cancel()
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(bitmap)

                    val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                        override fun onDoubleTap(e: MotionEvent?): Boolean {
                            openZoomFragment(bitmap)
                            return super.onDoubleTap(e)
                        }
                    })

                    imageView.setOnTouchListener { _, event ->
                        gestureDetector.onTouchEvent(event)
                        true
                    }
                }
            }, { error ->
                Timber.e(error)
                context?.run { showToast(this, "Loading failed $error") }
            })
        }
    }

    private fun openZoomFragment(bitmap: Bitmap) {
        childFragmentManager.commit {
            ZoomFragment().apply { image = bitmap }.show(childFragmentManager, null)
        }
    }

    /**
     * [ListAdapter] to get detailed tag information and displays them in two columns.
     *
     * @param post to get the tags for
     */
    class TagListAdapter(lifecycleScoop: CoroutineScope, val post: Post) : androidx.recyclerview.widget.RecyclerView.Adapter<TagListAdapter.TagViewHolder>(), KoinComponent {
        private var tagList = emptyList<Tag>()

        val dataSource: DataSource by inject()

        /**
         * Asynchronously loads the tag information.
         */
        init {
            tagList = post.getTagList()

            lifecycleScoop.launch {
                dataSource.loadTagColors(tagList)
                        .run {
                            tagList = this
                            notifyDataSetChanged()
                        }
            }
        }

        /**
         * Starts a new [ThumbnailActivity] with the given tags a search parameters.
         *
         * @param ctx to start the activity with
         * @param tag to search for
         */
        private fun searchForTag(ctx: Context, tag: String) {
            val intent = Intent(ctx, ThumbnailActivity::class.java)
            intent.putExtra(BOARD_INTENT_KEY, post.getBoard())
            intent.putExtra(TAGS_INTENT_KEY, tag)
            ctx.startActivity(intent)
        }

        override fun onBindViewHolder(p0: TagViewHolder, p1: Int) {
            p0.bindTag(tagList[p1])
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): TagViewHolder {
            val textView = TextView(p0.context)
//            textView.textSize = Constants.FONT_SIZE
            textView.gravity = Gravity.CENTER
            val padding = 5
            val scale = p0.resources.displayMetrics.density
            val dpAsPixels = (padding * scale).roundToInt()
            textView.setPadding(dpAsPixels, dpAsPixels, dpAsPixels, dpAsPixels)
            return TagViewHolder(textView)
        }

        /**
         * Returns the given position.
         *
         * @param position
         * @return position
         */
        override fun getItemId(position: Int): Long = position.toLong()

        /**
         * Returns the number of tags.
         *
         * @return number of tags
         */
        override fun getItemCount(): Int = tagList.size

        inner class TagViewHolder(val textView: TextView) : androidx.recyclerview.widget.RecyclerView.ViewHolder(textView) {

            fun bindTag(tag: Tag) {
                textView.text = tag.name
                tag.getTextColor()
                textView.setTextColor(tag.getTextColor())
                textView.setOnClickListener { searchForTag(textView.context, tag.name) }
            }
        }
    }
}