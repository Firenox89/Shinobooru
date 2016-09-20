package com.github.firenox89.shinobooru.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.model.Post
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
                        adapter = ArrayAdapter<String>(ctx, R.layout.listitem_tag, post.tags.split(" "))
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
}