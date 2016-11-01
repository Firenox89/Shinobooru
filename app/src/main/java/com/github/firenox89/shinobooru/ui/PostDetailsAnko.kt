package com.github.firenox89.shinobooru.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.app.Shinobooru
import com.github.firenox89.shinobooru.model.Post
import com.github.firenox89.shinobooru.model.Tag
import org.jetbrains.anko.*

/**
 * Creates the ui for the post details fragment.
 * This fragment contains information about the post, buttons to download and the tags.
 * @param post to take the details from
 */
class PostDetailsAnko<T>(val post: Post) : AnkoComponent<T> {
    companion object {
        val downloadIcon = BitmapFactory.decodeResource(Shinobooru.appContext.resources,
                                                        R.drawable.cloud_download_2_32x32)
    }

    /**
     * Creates the ui and returns it to the caller.
     */
    override fun createView(ui: AnkoContext<T>): View = with(ui) {
        verticalLayout {
            verticalLayout {
                textView {
                    text = "Size ${humanizeSize(post.file_size)}"
                }
                textView {
                    text = "Dimension ${post.width}x${post.height}"
                }
                imageButton {
                    imageBitmap = downloadIcon
                    onClick {
                        post.downloadFile()
                        toast("Download ${post.file_url}")
                    }
                }
            }
            //only adds these if there actually is a jpeg
            if (post.jpeg_file_size != 0) {
                verticalLayout {
                    textView {
                        text = "Jpeg Size ${humanizeSize(post.jpeg_file_size)}"
                    }
                    textView {
                        text = "Jpeg Dimension ${post.jpeg_width}x${post.jpeg_height}"
                    }
                    imageButton {
                        imageBitmap = downloadIcon
                        onClick {
                            post.downloadJpeg()
                            toast("Download ${post.jpeg_url}")
                        }
                    }
                }
            }
            textView {
                text = "Author ${post.author}"
            }
            if (post.source.isNotEmpty()) {
                linearLayout {
                    gravity = Gravity.CENTER
                    textView {
                        text = "Source"
                    }
                    textView {
                        text = Html.fromHtml("<a href=\"${post.source}\">${post.source}</a>")
                        movementMethod = LinkMovementMethod.getInstance()
                    }
                }
            }
            verticalLayout {
                gravity = Gravity.CENTER
                linearLayout {
                    textView {
                        text = "Tags"
                    }
                    listView {
                        adapter = TagListAdapter(post)
                    }
                }
            }
        }.applyRecursively { view ->
            when (view) {
                is TextView -> {
                    view.padding = dip(10)
                    view.gravity = Gravity.CENTER
                    view.textSize = 24.toFloat()
                }
                is ImageButton -> {
                    view.padding = dip(10)
                }
            }
        }
    }

    /**
     * Converts a byte value into a human readable value.
     *
     * @param size in bytes
     * @return value in kb/mb as string
     */
    private fun humanizeSize(size: Int): String {
        if (size > 1024 * 1024)
            return "${(size.toDouble() / (1024 * 1024)).format(2)} M"
        if (size > 1024)
            return "${(size.toDouble() / 1024).format(2)} K"
        return "$size"
    }

    /** Extension function to format a double value to the given number of digits */
    fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)

    /**
     * [ListAdapter] to get detailed tag information and displays them in two columns.
     *
     * @param post to get the tags for
     */
    class TagListAdapter(val post: Post) : BaseAdapter(), ListAdapter {
        var tagList = mutableListOf<Pair<Tag, Tag?>>()

        /**
         * Asynchronously loads the tag information.
         */
        init {
            doAsync {
                val tags = post.getTagList()
                //group the tags in pairs
                for (i in 0..tags.size - 1 step 2) {
                    if (i + 1 < tags.size)
                        tagList.add(Pair(tags[i], tags[i + 1]))
                    else
                        tagList.add(Pair(tags[i], null))
                }
                //notifies that don't come from the ui thread get ignored
                uiThread {
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
        fun searchForTag(ctx: Context, tag: String) {
            val intent = Intent(ctx, ThumbnailActivity::class.java)
            intent.putExtra("tags", tag)
            ctx.startActivity(intent)
        }

        /**
         * Composes a view out of the tag pair from the given position.
         * Second textview is omitted when the pair contains only one tag.
         *
         * @param position of the tag pair
         * @param convertView gets ignored
         * @param parent view
         */
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val layout = GridLayout(parent.context)
            val parentWidth = parent.width

            val firstTag = tagList[position].first
            val textView1 = TextView(parent.context)
            textView1.textSize = 24.toFloat()
            textView1.gravity = Gravity.CENTER
            textView1.padding = 5
            textView1.text = firstTag.name
            textView1.textColor = firstTag.getTextColor()
            textView1.onClick { searchForTag(parent.context, firstTag.name) }
            layout.addView(textView1, parentWidth / 2, -1)

            val secondTag = tagList[position].second
            if (secondTag != null) {
                val textView2 = TextView(parent.context)
                textView2.textSize = 24.toFloat()
                textView2.gravity = Gravity.CENTER
                textView2.padding = 5
                textView2.text = secondTag.name
                textView2.textColor = secondTag.getTextColor()
                textView2.onClick { searchForTag(parent.context, secondTag.name) }
                layout.addView(textView2, parentWidth / 2, -1)
            }

            return layout
        }

        /**
         * Returns the tag for the given position.
         *
         * @param position
         * @return tag for positon
         */
        override fun getItem(position: Int): Any {
            return tagList[position]
        }

        /**
         * Returns the given position.
         *
         * @param position
         * @return position
         */
        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        /**
         * Returns the number of tags.
         *
         * @return number of tags
         */
        override fun getCount(): Int {
            return tagList.size
        }
    }
}