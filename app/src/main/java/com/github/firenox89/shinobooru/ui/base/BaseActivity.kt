package com.github.firenox89.shinobooru.ui.base

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.github.firenox89.shinobooru.repo.DataSource
import com.github.firenox89.shinobooru.settings.SettingsActivity
import com.github.firenox89.shinobooru.ui.thumbnail.ThumbnailActivity
import com.github.firenox89.shinobooru.utility.Constants.BOARD_INTENT_KEY
import com.github.firenox89.shinobooru.utility.Constants.TAGS_INTENT_KEY
import org.koin.android.ext.android.inject

@SuppressLint("Registered")
open class BaseActivity: AppCompatActivity() {
    protected val dataSource: DataSource by inject()
    fun navigateToSync() {
    }

    fun navigateToSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    fun openBoard(board: String, tags: String? = null) {
        startActivity(Intent(this, ThumbnailActivity::class.java).apply {
            putExtra(BOARD_INTENT_KEY, board)
            tags?.let { putExtra(TAGS_INTENT_KEY, it)}
        })
    }
}