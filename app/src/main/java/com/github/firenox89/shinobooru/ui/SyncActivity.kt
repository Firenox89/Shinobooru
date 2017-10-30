package com.github.firenox89.shinobooru.ui

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.github.firenox89.shinobooru.cloud.GoogleDrive
import com.github.firenox89.shinobooru.model.DownloadedPost
import com.github.firenox89.shinobooru.utility.Constants
import com.github.firenox89.shinobooru.utility.FileManager
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.listeners.onClick

/**
 * Created by firenox on 1/21/17.
 */

class SyncActivity : Activity() {
    val TAG = "SyncActivity"
    var drive = GoogleDrive()

    override fun onCreate(savedInstanceState: Bundle?) {
        //TODO check permissions
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
                        linearLayout {
                            textViewsforBoard.put(it, textView("loading"))
                            textViewsforBoard.put(it + "green",
                                    textView {
                                        text = "0"
                                        setTextColor(Color.GREEN)
                                    })
                            textViewsforBoard.put(it + "red",
                                    textView {
                                        text = "0"
                                        setTextColor(Color.RED)
                                    })
                        }
                    }
                }
            }
            doAsync(Throwable::printStackTrace) {
                drive.collectData {
                    val data = it?.mapKeys { it.key.title }
                    if (data != null) {
                        runOnUiThread {
                            updateUI(textViewsforBoard, data)
                        }
                    }
                }
            }
            button("Sync") {
                onClick {
                    doAsync(Throwable::printStackTrace) {
                        drive.sync {
                            val data = it?.mapKeys { it.key.title }
                            if (data != null) {
                                runOnUiThread {
                                    updateUI(textViewsforBoard, data)
                                }
                            }

                        }
                    }
                }
            }
        }.applyRecursively { view ->
            when (view) {
                is TextView -> {
                    view.padding = dip(10)
                    view.gravity = Gravity.CENTER
                    view.textSize = Constants.FONT_SIZE
                }
                is LinearLayout -> {
                    view.gravity = Gravity.CENTER
                }
            }
        }

    }
    fun updateUI(textViewsforBoard: Map<String, TextView>, data: Map<String, List<DownloadedPost>>) {
        textViewsforBoard.forEach {
            val textView = it.value
            when {
                it.key.endsWith("green") -> {
                    val board = it.key.substring(0, it.key.length - 5)
                    doAsync {
                        val newText = "+${drive.getPostOnlyOnDrive(board)?.size}"
                        uiThread {
                            textView.text = newText
                        }
                    }
                }
                it.key.endsWith("red") -> {
                    val board = it.key.substring(0, it.key.length - 3)
                    doAsync {
                        val newText = "+${drive.getPostOnlyOnDevice(board)?.size}"
                        uiThread {
                            textView.text = newText
                        }
                    }
                }
                else -> {
                    val board = it.key
                    it.value.text = "${data[board]?.size}"
                }
            }
        }
    }
}