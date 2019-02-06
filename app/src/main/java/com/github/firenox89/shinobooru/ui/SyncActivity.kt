package com.github.firenox89.shinobooru.ui

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TextView
import com.github.firenox89.shinobooru.cloud.GoogleDrive
import com.github.firenox89.shinobooru.utility.Constants
import timber.log.Timber

/**
 * Created by firenox on 1/21/17.
 */

class SyncActivity : Activity() {
    val TAG = "SyncActivity"
    var drive = GoogleDrive(this)
    lateinit var table: TableLayout

    fun updateTable() {
        Timber.i("updateTable")/*
        runOnUiThread {
            try {
            cleanTable(table)
            drive.boards.forEach {
                table.tableRow {
                    textView(it.first)
                    textView(it.second.count().toString()) {
                        gravity = Gravity.CENTER
                    }
                    textView(drive.getPostOnlyOnDrive(it.first, it.second).count().toString()) {
                        gravity = Gravity.CENTER
                    }
                    textView(drive.getPostOnlyOnDevice(it.first, it.second)?.count().toString()) {
                        gravity = Gravity.CENTER
                    }
                }
            }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }*/
    }

    private fun cleanTable(table: TableLayout) {
        val childCount = table.childCount

        // Remove all rows except the first one
        if (childCount > 1) {
            table.removeViews(1, childCount - 1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        //TODO check permissions
        super.onCreate(savedInstanceState)
/*
        verticalLayout {
            gravity = Gravity.CENTER
            table = tableLayout {
                tableRow {
                    textView("On\n Drive")
                    textView("Drive\n only")
                    textView("Device\n only")
                }
            }
            button("Sync") {
                onClick {
                    doAsync(Throwable::printStackTrace) {
                        drive.syncDevice2Drive()
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
        drive.fetchData {
            updateTable()
        }*/
    }
}