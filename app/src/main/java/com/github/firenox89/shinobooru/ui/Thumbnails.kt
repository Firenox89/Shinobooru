package com.github.firenox89.shinobooru.ui

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.widget.DrawerLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.model.Post
import com.github.firenox89.shinobooru.model.PostLoader
import com.github.firenox89.shinobooru.settings.SettingsActivity
import com.github.salomonbrys.kodein.KodeinInjected
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotterknife.bindView
import org.jetbrains.anko.startActivity
import rx.subjects.PublishSubject

class Thumbnails : Activity(), KodeinInjected {

    override val injector = KodeinInjector()

    private val sharedPrefs: SharedPreferences by instance()
    private val recyclerLayout: StaggeredGridLayoutManager by instance()
    private val recyclerAdapter: PostRecyclerAdapter by instance()

    private val recyclerView: RecyclerView by bindView(R.id.recycleView)
    private val mDrawerLayout: DrawerLayout by bindView(R.id.drawer_layout)

    private val updateThumbnail: PublishSubject<Int> by instance("thumbnailUpdates")

    override fun onCreate(savedInstanceState: Bundle?) {
        //TODO: save and reload position
        super.onCreate(savedInstanceState)

        inject(appKodein())

        setContentView(R.layout.activity_thumbnails)

        //TODO: add header
        val mDrawerList = findViewById(R.id.left_drawer) as ListView

        val items: Array<String> = arrayOf("Settings", "FileView", "yande.re", "konachan.com")
        mDrawerList.adapter = ArrayAdapter(this, R.layout.drawer_list_item, items)

        mDrawerList.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            when (position) {
                0 -> openSettings()
                1 -> openFileView()
                2 -> setYandere()
                3 -> setKonachan()
            }
        }

        recyclerAdapter.getPositionClicks().subscribe {
            val post = PostLoader.getPostAt(it)
            val intent = Intent(this, PostPagerActivity::class.java)
            intent.putExtra(resources.getString(R.string.post_class), post)
            startActivityForResult(intent, 1)
        }

        recyclerView.adapter = recyclerAdapter
        updatePostPerRow(sharedPrefs.getString("post_per_row_list", "5").toInt())

        recyclerView.layoutManager = recyclerLayout
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val lastVisble = recyclerLayout.findLastCompletelyVisibleItemPositions(null).last()
                if (recyclerLayout.itemCount - lastVisble < recyclerLayout.spanCount + 1) {
                    //TODO: reduce the amount of fired events while scrolling
                    PostLoader.requestNextPosts()
                }
            }
        })

        val swipeRefresh = findViewById(R.id.swipe_refresh_layout) as SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener {
            PostLoader.onRefresh()
            swipeRefresh.isRefreshing = false
        }

        updateThumbnail.subscribe { updatePostPerRow(it) }
    }

    override fun onDestroy() {
        recyclerView.layoutManager = null
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val post = data?.getSerializableExtra(resources.getString(R.string.post_class)) as Post
        recyclerView.scrollToPosition(PostLoader.getPositionFor(post))
    }

    fun updatePostPerRow(value: Int) {
        recyclerLayout.spanCount = value
        recyclerAdapter.usePreview = value != 1
    }

    private fun setKonachan() {
        PostLoader.setBaseURL(SettingsActivity.konachanURL)
        mDrawerLayout.closeDrawers()
    }

    private fun setYandere() {
        PostLoader.setBaseURL(SettingsActivity.yandereURL)
        mDrawerLayout.closeDrawers()
    }

    private fun openFileView() {
        //TODO: loading animation
        PostLoader.setFileMode()
        mDrawerLayout.closeDrawers()
    }

    private fun openSettings() {
        startActivity<SettingsActivity>()
    }
}

