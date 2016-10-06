package com.github.firenox89.shinobooru.ui

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.TextView
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.model.Post
import com.github.firenox89.shinobooru.model.PostLoader
import com.github.firenox89.shinobooru.settings.SettingsActivity
import com.github.salomonbrys.kodein.KodeinInjected
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import org.jetbrains.anko.*
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.support.v4.drawerLayout
import org.jetbrains.anko.support.v4.swipeRefreshLayout
import rx.subjects.PublishSubject

class ThumbnailActivity : Activity(), KodeinInjected {

    override val injector = KodeinInjector()

    private val sharedPrefs: SharedPreferences by instance()

    private val recyclerLayout = StaggeredGridLayoutManager(4, StaggeredGridLayoutManager.VERTICAL)
    lateinit private var recyclerAdapter: ThumbnailAdapter
    lateinit private var recyclerView: RecyclerView
    lateinit private var mDrawerLayout: DrawerLayout

    private val updateThumbnail: PublishSubject<Int> by instance("thumbnailUpdates")

    override fun onCreate(savedInstanceState: Bundle?) {
        //TODO: save and reload position
        super.onCreate(savedInstanceState)
        val tags = intent.getStringExtra("tags") ?: ""
        val postLoader = PostLoader.getLoader(SettingsActivity.currentBoardURL, tags)
        recyclerAdapter = ThumbnailAdapter(postLoader)

        inject(appKodein())

        drawerLayout {
            mDrawerLayout = this
            fitsSystemWindows = true
            swipeRefreshLayout {
                setOnRefreshListener {
                    recyclerAdapter.postLoader.onRefresh()
                    isRefreshing = false
                }
                recyclerView {
                    recyclerView = this
                    adapter = recyclerAdapter
                    layoutManager = recyclerLayout
                }
            }
            listView {
                //TODO: add header
                adapter = MenuDrawerAdapter()
                lparams(width = 500, height = matchParent, gravity = Gravity.START)
                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                backgroundColor = Color.parseColor("#111111")
                dividerHeight = 0
                divider.alpha = 0

                onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
                    when (position) {
                        0 -> openSettings()
                        1 -> openFileView()
                        2 -> setYandere()
                        3 -> setKonachan()
                    }
                }
            }
        }

        recyclerAdapter.getPositionClicks().subscribe {
            val post = recyclerAdapter.postLoader.getPostAt(it)
            val intent = Intent(this, PostPagerActivity::class.java)
            intent.putExtra("board", recyclerAdapter.postLoader.board)
            intent.putExtra("tags", recyclerAdapter.postLoader.tags)
            intent.putExtra(resources.getString(R.string.post_class), post)
            startActivityForResult(intent, 1)
        }

        updatePostPerRow(sharedPrefs.getString("post_per_row_list", "5").toInt())

        updateThumbnail.subscribe { updatePostPerRow(it) }
    }

    override fun onDestroy() {
        recyclerView.layoutManager = null
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val post = data?.getSerializableExtra(resources.getString(R.string.post_class)) as Post
        recyclerView.scrollToPosition(recyclerAdapter.postLoader.getPositionFor(post))
    }

    fun updatePostPerRow(value: Int) {
        recyclerLayout.spanCount = value
        recyclerAdapter.usePreview = value != 1
    }

    private fun setKonachan() {
        mDrawerLayout.closeDrawers()
        recyclerView.scrollTo(0, 0)
        recyclerAdapter.resetPostLoader(PostLoader.getLoader(SettingsActivity.konachanURL))
    }

    private fun setYandere() {
        mDrawerLayout.closeDrawers()
        recyclerView.scrollTo(0, 0)
        recyclerAdapter.resetPostLoader(PostLoader.getLoader(SettingsActivity.yandereURL))
    }

    private fun openFileView() {
        //TODO: loading animation
        mDrawerLayout.closeDrawers()
        recyclerView.scrollToPosition(1)
        recyclerAdapter.resetPostLoader(PostLoader.getLoader("FileLoader"))
    }

    private fun openSettings() {
        startActivity<SettingsActivity>()
    }

    class MenuDrawerAdapter: BaseAdapter() {
        val items: Array<String> = arrayOf("Settings", "FileView", "yande.re", "konachan.com")

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val textView = TextView(parent?.context)
            textView.textSize = 24F
            textView.gravity = Gravity.CENTER
            textView.padding = 10
            textView.text = items[position]
            return textView
        }

        override fun getItem(position: Int): Any {
            return items[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return items.size
        }

    }
}

