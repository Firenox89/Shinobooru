package com.github.firenox89.shinobooru.ui.base

import android.annotation.SuppressLint
import android.content.Intent
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.AppCompatActivity
import com.github.firenox89.shinobooru.repo.DataSource
import com.github.firenox89.shinobooru.settings.SettingsActivity
import com.github.firenox89.shinobooru.ui.GoogleSignInActivity
import com.github.firenox89.shinobooru.ui.thumbnail.ThumbnailActivity
import com.github.firenox89.shinobooru.utility.Constants.BOARD_INTENT_KEY
import com.github.firenox89.shinobooru.utility.Constants.FILE_LOADER_NAME
import org.koin.android.ext.android.inject

@SuppressLint("Registered")
open class RxActivity: AppCompatActivity() {
    protected val dataSource: DataSource by inject()

    fun setupDrawer(navigation: NavigationView, drawer: androidx.drawerlayout.widget.DrawerLayout) {
        dataSource.getBoards().forEach {
            navigation.menu.add(it)
        }
        navigation.setNavigationItemSelectedListener { item ->
            when (item.title) {
                "Settings" -> {
                    drawer.closeDrawers()
                    navigateToSettings()
                }
                "Sync" -> {
                    drawer.closeDrawers()
                    navigateToSync()
                }
                "Filemanager" -> {
                    drawer.closeDrawers()
                    openBoard(FILE_LOADER_NAME)
                }
                else -> {
                    drawer.closeDrawers()
                    openBoard(item.title.toString())
                }
            }
            true
        }
    }

    private fun navigateToSync() {
        val intent = Intent(this, GoogleSignInActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun openBoard(board: String) {
        val intent = Intent(this, ThumbnailActivity::class.java)
        intent.putExtra(BOARD_INTENT_KEY, board)
        startActivity(intent)
    }
}