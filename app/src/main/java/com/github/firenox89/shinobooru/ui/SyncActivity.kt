package com.github.firenox89.shinobooru.ui

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.github.firenox89.shinobooru.file.GoogleDrive
import com.github.firenox89.shinobooru.model.FileManager
import com.github.firenox89.shinobooru.model.Post
import org.jetbrains.anko.*

/**
 * Created by firenox on 1/21/17.
 */

class SyncActivity : Activity() {
    val TAG = "SyncActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val boards = FileManager.boards.map { it.key }

        verticalLayout {
            gravity = Gravity.CENTER
            textView("Post on device:")
            linearLayout {
                boards.forEach {
                    verticalLayout {
                        textView(it)
                        textView {
                            text = "${FileManager.boards[it]?.size}"
                        }
                    }
                }
            }
            textView("Post on drive:")

            val textViewsforBoard = mutableMapOf<String, TextView>()
            linearLayout {
                boards.forEach {
                    verticalLayout {
                        textView(it)
                        textViewsforBoard.put(it, textView("loading"))
                    }
                }
            }
            var drive = GoogleDrive(this@SyncActivity) {
                val data = it?.mapKeys { it.key.title }
                if (data != null) {
                    runOnUiThread {
                        textViewsforBoard.forEach {
                            val board = it.key
                            it.value.text = "${data[board]?.size}"
                        }
                    }
                }
            }
            button("Sync") {
                onClick {
                    drive.sync()
                }
            }
        }.applyRecursively { view ->
            when (view) {
                is TextView -> {
                    view.padding = dip(10)
                    view.gravity = Gravity.CENTER
                    view.textSize = 24.toFloat()
                }
                is LinearLayout -> {
                    view.gravity = Gravity.CENTER
                }
            }
        }

    }
}