package com.github.firenox89.shinobooru.ui.post

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListAdapter
import android.widget.TextView
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.repo.DataSource
import com.github.firenox89.shinobooru.repo.PostLoader
import com.github.firenox89.shinobooru.repo.model.Post
import com.github.firenox89.shinobooru.repo.model.Tag
import com.github.firenox89.shinobooru.ui.thumbnail.ThumbnailActivity
import com.github.firenox89.shinobooru.utility.Constants.BOARD_INTENT_KEY
import com.github.firenox89.shinobooru.utility.Constants.TAGS_INTENT_KEY
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.math.roundToInt

/**
 * Contains the two child fragments.
 */
class PostFragment : androidx.fragment.app.Fragment() {
    val dataSource: DataSource by inject()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val board = arguments!!.getString("board")
        val tags = arguments!!.getString("tags")
        val posi = arguments!!.getInt("posi")

        val layout = inflater.inflate(R.layout.fragment_post, null)
        val postLoader = dataSource.getPostLoader(board, tags)
        val post: Post = postLoader.getPostAt(posi)

        val imageview = layout.findViewById<ImageView>(R.id.postimage)
        val authorText = layout.findViewById<TextView>(R.id.authorText)
        val sourceText = layout.findViewById<TextView>(R.id.sourceText)
        val postText = layout.findViewById<TextView>(R.id.postInfoText)

        val tagListView = layout.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.tagList)
        tagListView.adapter = TagListAdapter(postLoader, post)
        tagListView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(context, 2)

        authorText.text = String.format(resources.getText(R.string.author_s).toString(), post.author)
        sourceText.text = String.format(resources.getText(R.string.source_s).toString(), post.source)
        postText.text = String.format(resources.getText(R.string.board_s_id_s).toString(), post.getBoard(), post.id)

        GlobalScope.launch {
            //display preview image first for faster response
            postLoader.loadPreview(post).let { bitmap ->
                imageview.setImageBitmap(bitmap)
            }
            postLoader.loadSample(post)
                    .let { bitmap ->
                        //TODO cancel loadPreview
                        imageview.setImageBitmap(bitmap)
                    }
        }

        return layout
    }

    /**
     * [ListAdapter] to get detailed tag information and displays them in two columns.
     *
     * @param post to get the tags for
     */
    class TagListAdapter(postLoader: PostLoader, val post: Post) : androidx.recyclerview.widget.RecyclerView.Adapter<TagListAdapter.TagViewHolder>() {
        private var tagList = mutableListOf<Tag>()

        /**
         * Asynchronously loads the tag information.
         */
        init {
            GlobalScope.launch {
                postLoader.getTagList(post)
                        .let { tags ->
                            tagList.clear()
                            tagList.addAll(tags)
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