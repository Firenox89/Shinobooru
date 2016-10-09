package com.github.firenox89.shinobooru.ui

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.model.ApiWrapper
import com.github.firenox89.shinobooru.model.Post
import com.github.firenox89.shinobooru.model.PostLoader
import com.github.firenox89.shinobooru.model.Tag
import com.github.firenox89.shinobooru.settings.SettingsActivity
import com.github.salomonbrys.kodein.KodeinInjected
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.google.gson.Gson
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
    lateinit private var menuDrawerLayout: DrawerLayout

    private val updateThumbnail: PublishSubject<Int> by instance("thumbnailUpdates")

    override fun onCreate(savedInstanceState: Bundle?) {
        //TODO: save and reload position
        super.onCreate(savedInstanceState)
        val tags = intent.getStringExtra("tags") ?: ""
        val postLoader = PostLoader.getLoader(SettingsActivity.currentBoardURL, tags)
        recyclerAdapter = ThumbnailAdapter(postLoader)

        inject(appKodein())

        drawerLayout {
            menuDrawerLayout = this
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
            //right drawer
            listView {
                //TODO: add header
                adapter = MenuDrawerAdapter()
                lparams(width = 500, height = matchParent, gravity = Gravity.START)
                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                backgroundColor = Color.parseColor("#111111")
                alpha = 0.7F
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
            //left drawer
            linearLayout {
                lparams(width = 500, height = matchParent, gravity = Gravity.END)
                backgroundColor = Color.parseColor("#111111")
                alpha = 0.7F
                linearLayout {
                    orientation = LinearLayout.VERTICAL
                    autoCompleteTextView {
                        setAdapter(TagSearchAutoCompleteAdapter())
                        //start autocomplete after the third letter
                        threshold = 3
                        hint = "Search..."
                        setOnEditorActionListener { textView, i, keyEvent ->
                            val intent = Intent(ctx, ThumbnailActivity::class.java)
                            intent.putExtra("tags", textView.text.toString())
                            ctx.startActivity(intent)

                            //consume
                            true
                        }

                    }
                    listView {
                        //TODO: add previous searches
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
        menuDrawerLayout.closeDrawers()
        recyclerView.scrollTo(0, 0)
        recyclerAdapter.resetPostLoader(PostLoader.getLoader(SettingsActivity.konachanURL))
    }

    private fun setYandere() {
        menuDrawerLayout.closeDrawers()
        recyclerView.scrollTo(0, 0)
        recyclerAdapter.resetPostLoader(PostLoader.getLoader(SettingsActivity.yandereURL))
    }

    private fun openFileView() {
        //TODO: loading animation
        menuDrawerLayout.closeDrawers()
        recyclerView.scrollToPosition(1)
        recyclerAdapter.resetPostLoader(PostLoader.getLoader("FileLoader"))
    }

    private fun openSettings() {
        startActivity<SettingsActivity>()
    }

    class MenuDrawerAdapter : BaseAdapter() {
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

    inner class TagSearchAutoCompleteAdapter : BaseAdapter(), Filterable {
        val tagList = mutableListOf<Tag>()

        override fun getItem(position: Int): Any {
            return tagList[position].name
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return tagList.size
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val results = FilterResults()

                    //constraint can be null
                    if (!constraint.isNullOrBlank()) {
                        //tags are always lower case
                        val name = constraint.toString().toLowerCase().trim()
                        val jsonResponse = ApiWrapper.requestTag(recyclerAdapter.postLoader.board, name)
                        val tags = Gson().fromJson<Array<Tag>>(jsonResponse, Array<Tag>::class.java)

                        //TODO: could be settable
                        val numberOfResult = 10
                        val sortedTagList = mutableListOf<Tag>()

                        val primaryMatches = tags.filter { it.name.startsWith(name) }
                        val secondaryMatches = tags.filter {
                            //split into words, drop the first word since it is already covered in primaryMatches
                            val words = it.name.split("_").drop(1)

                            //true if at least one word matches
                            words.filter { it.startsWith(name) }.any()
                        }
                        //add primaryMatches
                        sortedTagList.addAll(primaryMatches.take(numberOfResult))

                        //not enough results yet, try to fill the rest with secondaryMatches
                        if (sortedTagList.size < numberOfResult)
                            sortedTagList.addAll(secondaryMatches.take(numberOfResult - primaryMatches.size))

                        //still not enough matches, add rest
                        if (sortedTagList.size < numberOfResult)
                        {
                            val matchesLeft = numberOfResult - primaryMatches.size - secondaryMatches.size
                            //remove primary and secondary matches to not add them twice
                            val restOfResults = tags.subtract(primaryMatches).subtract(secondaryMatches)
                            sortedTagList.addAll(restOfResults.take(matchesLeft))
                        }

                        results.count = numberOfResult
                        //take only the first entries
                        results.values = sortedTagList
                    }

                    return results
                }

                override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                    if (results.values is List<*>) {
                        val tags = results.values as List<Tag>
                        tagList.clear()
                        tagList.addAll(tags)
                        notifyDataSetChanged()
                    }
                }
            }
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val textView = TextView(parent.context)
            textView.text = tagList[position].name
            textView.gravity = Gravity.CENTER
            textView.textSize = 20F
            textView.textColor = tagList[position].getTextColor()
            textView.backgroundColor = Color.BLACK
            return textView
        }
    }
}

