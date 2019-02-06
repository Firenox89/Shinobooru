package com.github.firenox89.shinobooru.ui.base

import android.annotation.SuppressLint
import android.content.Intent
import android.support.design.widget.NavigationView
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import com.github.firenox89.shinobooru.repo.DataSource
import com.github.firenox89.shinobooru.settings.SettingsActivity
import com.github.firenox89.shinobooru.ui.GoogleSignInActivity
import com.github.firenox89.shinobooru.ui.thumbnail.ThumbnailActivity
import com.github.firenox89.shinobooru.utility.Constants.BOARD_INTENT_KEY
import com.github.firenox89.shinobooru.utility.Constants.FILE_LOADER_NAME
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import org.koin.android.ext.android.inject

@SuppressLint("Registered")
open class RxActivity: AppCompatActivity() {
    private  val disposeables = CompositeDisposable()
    protected val dataSource: DataSource by inject()

    fun subscribe(disposable: Disposable) {
        disposeables.add(disposable)
    }

    override fun onDestroy() {
        disposeables.dispose()
        super.onDestroy()
    }

    fun setupDrawer(navigation: NavigationView, drawer: DrawerLayout) {
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