package com.github.firenox89.shinobooru.ui.post

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.app.Shinobooru
import com.github.firenox89.shinobooru.cloud.GoogleDrive
import com.github.firenox89.shinobooru.model.DownloadedPost
import com.github.firenox89.shinobooru.model.Post
import com.github.firenox89.shinobooru.model.Tag
import com.github.firenox89.shinobooru.ui.thumbnail.ThumbnailActivity
import com.github.firenox89.shinobooru.utility.Constants
import com.github.firenox89.shinobooru.utility.FileManager
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.listeners.onClick
import android.R.attr.button
import android.util.Log
import android.widget.RelativeLayout


/**
 * Creates the ui for the post details fragment.
 * This fragment contains information about the post, buttons to download and the tags.
 * @param post to take the details from
 */
class PostDetailsAnko(val post: Post) : AnkoComponent<PostDetailsFragment> {
    companion object {
        val downloadIcon = BitmapFactory.decodeResource(Shinobooru.appContext.resources,
                                                        R.drawable.cloud_download_2_32x32)
    }
    var downloadButton: ImageButton? = null

    /**
     * Creates the ui and returns it to the caller.
     */
    override fun createView(ui: AnkoContext<PostDetailsFragment>): View = with(ui) {
        val downloadedPost = post is DownloadedPost

        verticalLayout {
            dividerPadding = 20
            if (!downloadedPost) {
                verticalLayout {
                    background = resources.getDrawable(R.drawable.roundcorners, null)

                    textView {
                        text = "Author ${post.author}"
                    }
                    if (post.source.isNotEmpty()) {
                        linearLayout {
                            gravity = Gravity.CENTER
                            textView {
                                text = "Source "
                            }
                            textView {
                                text = Html.fromHtml("<a href=\"${post.source}\">${post.source}</a>")
                                movementMethod = LinkMovementMethod.getInstance()
                            }
                        }
                    }
                }
            } else {
                verticalLayout {
                    button("Delete") {
                        onClick {
                            alert {
                                title = "Really delete this port?"
                                positiveButton("Yep") {
                                    FileManager.deleteDownloadedPost(post as DownloadedPost)
                                    toast("Post ${post.id} deleted")
                                }
                                negativeButton(android.R.string.no) {}
                            }
                        }
                    }
                    button("Delete from Google Drive") {
                        onClick {
                            GoogleDrive.delete(post as DownloadedPost)
                        }
                    }
                }
            }
            verticalLayout {
                background = resources.getDrawable(R.drawable.roundcorners)
                clipToOutline = true
                gravity = Gravity.CENTER
                textView {
                    text = "Post"
                }
                textView {
                    text = "Board ${post.getBoard()}"
                }
                textView {
                    text = "ID ${post.id}"
                }
            }
            verticalLayout {
                background = resources.getDrawable(R.drawable.roundcorners)
                clipToOutline = true
                gravity = Gravity.CENTER
                textView {
                    text = "Tags"
                }
                listView {
                    adapter = TagListAdapter(post)
                }
            }
            verticalLayout {
                background = resources.getDrawable(R.drawable.roundcorners)
//                relativeLayout {
//                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
//                                                          ViewGroup.LayoutParams.MATCH_PARENT)
//
//                    verticalLayout {
//                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
//                                                              ViewGroup.LayoutParams.MATCH_PARENT)
//                        textView {
//                            text = "Size ${humanizeSize(post.file_size)}"
//                        }
//                        textView {
//                            text = "Dimension ${post.width}x${post.height}"
//                        }
//                    }
//                    if (!downloadedPost) {
//                        relativeLayout {
//                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
//                                                                  ViewGroup.LayoutParams.WRAP_CONTENT)
//
//                            imageButton {
//                                imageBitmap = downloadIcon
//                                onClick {
//                                    val downloadResult = post.downloadFile()
//                                    toast(downloadResult ?: "Download ${post.file_url}")
//                                }
//                            }
//                        }
//                    }
//                }
                tableLayout {
                    tableRow {
                        verticalLayout {
                            textView {
                                text = "Size ${humanizeSize(post.file_size)}"
                            }
                            textView {
                                text = "Dimension ${post.width}x${post.height}"
                            }
                        }
                        if (!downloadedPost) {
                            relativeLayout {
                                downloadButton = imageButton {
                                    imageBitmap = downloadIcon
                                    onClick {
                                        val downloadResult = post.downloadFile()
                                        toast(downloadResult ?: "Download ${post.file_url}")
                                    }
                                    Log.e("layout", "${this.layoutParams}")
                                }
                            }
                        }
                    }
                }
                //only adds these if there actually is a jpeg
                if (post.jpeg_file_size != 0) {
                    linearLayout {
                        verticalLayout {
                            textView {
                                text = "Jpeg Size ${humanizeSize(post.jpeg_file_size)}"
                            }
                            textView {
                                text = "Jpeg Dimension ${post.jpeg_width}x${post.jpeg_height}"
                            }
                        }
                        imageButton {
                            imageBitmap = downloadIcon
                            onClick {
                                val downloadResult = post.downloadJpeg()
                                toast(downloadResult ?: "Download ${post.jpeg_url}")
                            }
                        }
                    }
                }
            }
        }.applyRecursively { view ->
            when (view) {
                is TextView -> {
                    view.gravity = Gravity.CENTER
                    view.textSize = Constants.FONT_SIZE
                }
                is LinearLayout -> {
                    view.padding = 20
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
        private var tagList = mutableListOf<Pair<Tag, Tag?>>()

        /**
         * Asynchronously loads the tag information.
         */
        init {
            doAsync(Throwable::printStackTrace) {
                val tags = post.getTagList()
                //group the tags in pairs
                for (i in 0 until tags.size step 2) {
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
        private fun searchForTag(ctx: Context, tag: String) {
            val intent = Intent(ctx, ThumbnailActivity::class.java)
            intent.putExtra("board", post.getBoard())
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
            textView1.textSize = Constants.FONT_SIZE
            textView1.gravity = Gravity.CENTER
            textView1.padding = 5
            textView1.text = firstTag.name
            textView1.textColor = firstTag.getTextColor()
            textView1.onClick { searchForTag(parent.context, firstTag.name) }
            layout.addView(textView1, parentWidth / 2, -1)

            val secondTag = tagList[position].second
            if (secondTag != null) {
                val textView2 = TextView(parent.context)
                textView2.textSize = Constants.FONT_SIZE
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
        override fun getItem(position: Int): Any = tagList[position]

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
        override fun getCount(): Int = tagList.size
    }
}