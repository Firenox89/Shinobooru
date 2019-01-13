package com.github.firenox89.shinobooru.ui.post

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.model.Post
import com.github.firenox89.shinobooru.utility.PostLoader
import com.github.firenox89.shinobooru.ext.defaultSchedulers
import com.github.firenox89.shinobooru.model.Tag
import com.github.firenox89.shinobooru.ui.thumbnail.ThumbnailActivity
import com.github.firenox89.shinobooru.utility.Constants
import com.github.firenox89.shinobooru.utility.Constants.BOARD_INTENT_KEY
import com.github.firenox89.shinobooru.utility.Constants.TAGS_INTENT_KEY
import timber.log.Timber
import kotlin.math.roundToInt

/**
 * Contains the two child fragments.
 */
class PostFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val board = arguments!!.getString("board")
        val tags = arguments!!.getString("tags")
        val posi = arguments!!.getInt("posi")

        val layout = inflater.inflate(R.layout.fragment_post, null)
        val postLoader = PostLoader.getLoader(board, tags)
        val post: Post = postLoader.getPostAt(posi)

        val imageview = layout.findViewById<ImageView>(R.id.postimage)
        val authorText = layout.findViewById<TextView>(R.id.authorText)
        val sourceText = layout.findViewById<TextView>(R.id.sourceText)
        val postText = layout.findViewById<TextView>(R.id.postInfoText)

        val tagListView = layout.findViewById<RecyclerView>(R.id.tagList)
        tagListView.adapter = TagListAdapter(postLoader, post)
        tagListView.layoutManager = GridLayoutManager(context, 2)

        authorText.text = String.format(resources.getText(R.string.author_s).toString(), post.author)
        sourceText.text = String.format(resources.getText(R.string.source_s).toString(), post.source)
        postText.text = String.format(resources.getText(R.string.board_s_id_s).toString(), post.getBoard(), post.id)
        //display preview image first for faster response
        val disposable = postLoader.loadPreview(post)
                .defaultSchedulers()
                .subscribe { bitmap ->
                    imageview.setImageBitmap(bitmap)
                }
        val disposable2 = postLoader.loadSample(post)
                .defaultSchedulers()
                .subscribe { bitmap ->
                    disposable.dispose()
                    imageview.setImageBitmap(bitmap)
                }

        return layout
    }

    /**
     * [ListAdapter] to get detailed tag information and displays them in two columns.
     *
     * @param post to get the tags for
     */
    class TagListAdapter(postLoader: PostLoader, val post: Post) : RecyclerView.Adapter<TagListAdapter.TagViewHolder>() {
        private var tagList = mutableListOf<Tag>()

        /**
         * Asynchronously loads the tag information.
         */
        init {
            postLoader.getTagList(post).defaultSchedulers()
                    .subscribe { tags ->
                        tagList.clear()
                        tagList.addAll(tags)
                        notifyDataSetChanged()
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

        inner class TagViewHolder(val textView: TextView): RecyclerView.ViewHolder(textView) {

            fun bindTag(tag: Tag)  {
                textView.text = tag.name
                tag.getTextColor()
                textView.setTextColor(tag.getTextColor())
                textView.setOnClickListener { searchForTag(textView.context, tag.name) }
            }
        }
    }
}