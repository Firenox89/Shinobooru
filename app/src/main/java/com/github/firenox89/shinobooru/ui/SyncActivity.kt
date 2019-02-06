package com.github.firenox89.shinobooru.ui

import android.os.Bundle
import android.support.v7.app.ActionBar
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.cloud.CloudSync
import com.github.firenox89.shinobooru.ext.defaultSchedulers
import com.github.firenox89.shinobooru.ui.base.RxActivity
import kotlinx.android.synthetic.main.activity_sync.*
import org.koin.android.ext.android.inject

/**
 * Created by firenox on 1/21/17.
 */

class SyncActivity : RxActivity() {
    private val cloud: CloudSync by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        //TODO check permissions
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sync)
        setSupportActionBar(findViewById(R.id.toolbar_sync))
        val actionbar: ActionBar? = supportActionBar
        actionbar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)
        }

        setupDrawer(nav_sync, drawer_sync)

        subscribe(cloud.fetchData().defaultSchedulers().subscribe { cloudBoards ->

        })

        dataSource.getAllPosts().forEach { board, posts ->

        }
    }
}