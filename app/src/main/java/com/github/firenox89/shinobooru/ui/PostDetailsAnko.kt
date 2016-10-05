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
import com.github.firenox89.shinobooru.model.Post
import com.github.firenox89.shinobooru.model.Tag
import org.jetbrains.anko.*

class PostDetailsAnko<T>(val post: Post) : AnkoComponent<T> {
    override fun createView(ui: AnkoContext<T>): View = with(ui) {
        val downloadIcon = BitmapFactory.decodeResource(ctx.resources, R.drawable.cloud_download_2_32x32)
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
                        showToast(ctx, post.file_url)
                    }
                }
            }
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
                            showToast(ctx, post.file_url)
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

                        onItemClick {
                            adapterView, view, i, l ->
                            searchForTag(ctx, (view as TextView).text.toString())
                        }
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

    fun searchForTag(ctx: Context, tag: String) {
        Log.e("PDA", "tags $tag")
        val intent = Intent(ctx, ThumbnailActivity::class.java)
        intent.putExtra("tags", tag)
        ctx.startActivity(intent)
    }

    fun showToast(context: Context, url: String) {
        val text = "Download $url"
        val duration = Toast.LENGTH_SHORT

        val toast = Toast.makeText(context, text, duration)
        toast.show()
    }

    private fun humanizeSize(size: Int): String {
        if (size > 1024 * 1024)
            return "${(size.toDouble() / (1024 * 1024)).format(2)} M"
        if (size > 1024)
            return "${(size.toDouble() / 1024).format(2)} K"
        return "$size"
    }

    fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)

    class TagListAdapter(val post: Post) : BaseAdapter(), ListAdapter {
        var tagList = mutableListOf<Pair<Tag, Tag?>>()

        init {
            doAsync {
                val tags = post.getTagList()
                for (i in 0..tags.size - 1 step 2) {
                    if (i + 1 < tags.size)
                        tagList.add(Pair(tags[i], tags[i + 1]))
                    else
                        tagList.add(Pair(tags[i], null))
                }
                uiThread {
                    notifyDataSetChanged()
                }
            }
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val layout = GridLayout(parent?.context)
            val parentWithd = parent?.width ?: 400

            val firstTag = tagList[position].first
            val textView1 = TextView(parent?.context)
            textView1.textSize = 24.toFloat()
            textView1.gravity = Gravity.CENTER
            textView1.padding = 5
            textView1.text = firstTag.name
            textView1.textColor = firstTag.textColor
            layout.addView(textView1, parentWithd / 2, -1)

            val secondTag = tagList[position].second
            if (secondTag != null) {
                val textView2 = TextView(parent?.context)
                textView2.textSize = 24.toFloat()
                textView2.gravity = Gravity.CENTER
                textView2.padding = 5
                textView2.text = secondTag.name
                textView2.textColor = secondTag.textColor
                layout.addView(textView2, parentWithd / 2, -1)
            }

            return layout
        }

        override fun getItem(position: Int): Any {
            return tagList[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return tagList.size
        }
    }
}