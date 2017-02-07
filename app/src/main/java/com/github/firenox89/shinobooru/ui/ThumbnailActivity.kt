package com.github.firenox89.shinobooru.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
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
import com.github.firenox89.shinobooru.model.*
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

/**
 * Contains a [DrawerLayout] with a menu drawer on the left and a drawer for searching on the right,
 * in the middle the is a [RecyclerView] with thumbnails of posts.
 */
class ThumbnailActivity : Activity(), KodeinInjected {

    val TAG = "ThumbnailActivity"
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

        inject(appKodein())

        //setup the RecyclerView adapter
        val tags = intent.getStringExtra("tags") ?: ""
        val board = intent.getStringExtra("board") ?: SettingsActivity.currentBoardURL
        val postLoader = PostLoader.getLoader(board, tags)
        recyclerAdapter = ThumbnailAdapter(postLoader)

        //when an image was clicked start a new PostPagerActivity that starts on Post that was clicked
        recyclerAdapter.onImageClickStream.subscribe {
            val post = recyclerAdapter.postLoader.getPostAt(it)
            val intent = Intent(this, PostPagerActivity::class.java)
            intent.putExtra("board", recyclerAdapter.postLoader.board)
            intent.putExtra("tags", recyclerAdapter.postLoader.tags)
            intent.putExtra(resources.getString(R.string.post_class), post)
            startActivityForResult(intent, 1)
        }

        //update the number of posts per row of the recycler layout
        updatePostPerRow(sharedPrefs.getString("post_per_row_list", "3").toInt())
        updateThumbnail.subscribe { updatePostPerRow(it) }

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
            //left drawer
            listView {
                addHeaderView(buildHeader(ctx))
                adapter = MenuDrawerAdapter()
                lparams(width = 500, height = matchParent, gravity = Gravity.START)
                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                backgroundColor = Color.parseColor("#111111")
                alpha = 0.7F
                dividerHeight = 0
                divider.alpha = 0

                onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
                    when (position) {
                        1 -> openSettings()
                        2 -> openFileView()
                        3 -> openGoogleDriveView()
                        4 -> setYandere()
                        5 -> setKonachan()
                    }
                }
            }
            //right drawer
            linearLayout {
                lparams(width = 500, height = matchParent, gravity = Gravity.END)
                backgroundColor = Color.parseColor("#111111")
                alpha = 0.7F
                linearLayout {
                    orientation = LinearLayout.VERTICAL
                    autoCompleteTextView {
                        setAdapter(TagSearchAutoCompleteAdapter(recyclerAdapter))
                        //start autocomplete after the third letter
                        threshold = 3
                        hint = "Search..."
                        setOnEditorActionListener { textView, i, keyEvent ->
                            val intent = Intent(ctx, ThumbnailActivity::class.java)
                            intent.putExtra("tags", textView.text.toString())
                            intent.putExtra("board", recyclerAdapter.postLoader.board)
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
    }

    fun buildHeader(context: Context): View {
        val header = LinearLayout(context)
        val imageView = ImageView(header.context)
        val image = BitmapFactory.decodeResource(resources, R.drawable.shinobu_header)
        imageView.setImageBitmap(image)
        header.addView(imageView)
        imageView.layoutParams = LinearLayout.LayoutParams(500, 300)
        //TODO this does not work as intended
        header.isClickable = false
        return header
    }

    /**
     * Removes the layout manager so it can be assigned again
     */
    override fun onDestroy() {
        PostLoader.discardLoader(recyclerAdapter.postLoader)
        recyclerView.layoutManager = null
        super.onDestroy()
    }

    /**
     * Gets called when a [PostPagerActivity] returns.
     * Scrolls the [RecyclerView] to the post position of the last view post of [PostPagerActivity].
     *
     * @param requestCode gets ignored
     * @param resultCode gets ignored
     * @param data if an Int value for position is stored there it will be used to scroll
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val post = data?.getIntExtra("position", -1)
        if (post != null && post != -1)
            recyclerView.scrollToPosition(post)
    }

    /**
     * Updates the span count of the layout manager.
     * If the given value is 1 the [RecyclerView] will display sample instead of preview images.
     * @param value that should be used as spanCount
     */
    fun updatePostPerRow(value: Int) {
        recyclerLayout.spanCount = value
        recyclerAdapter.usePreview = value != 1
    }

    private fun setKonachan() {
        menuDrawerLayout.closeDrawers()
        recyclerView.scrollTo(0, 0)
        recyclerAdapter.changePostLoader(PostLoader.getLoader(SettingsActivity.konachanURL))
    }

    /**
     * Close drawers, scroll the [RecyclerView] to the beginning
     * and sets [SettingsActivity#yandereURL] as Post Source.
     */
    private fun setYandere() {
        menuDrawerLayout.closeDrawers()
        recyclerView.scrollTo(0, 0)
        recyclerAdapter.changePostLoader(PostLoader.getLoader(SettingsActivity.yandereURL))
    }

    /**
     * Close drawers, scroll the [RecyclerView] to the beginning
     * and sets [FileLoader] as Post Source.
     */
    private fun openFileView() {
        //TODO: loading animation
        menuDrawerLayout.closeDrawers()
        recyclerView.scrollTo(0, 0)
        recyclerAdapter.changePostLoader(PostLoader.getLoader("FileLoader"))
    }

    /**
     * Starts the [SyncActivity].
     */
    private fun openGoogleDriveView() {
        menuDrawerLayout.closeDrawers()
        val intent = Intent(this, SyncActivity::class.java)
        startActivity(intent)
    }

    /**
     * Starts the [SettingsActivity].
     */
    private fun openSettings() {
        menuDrawerLayout.closeDrawers()
        startActivity<SettingsActivity>()
    }

    /**
     * [ListAdapter] for the menu drawer.
     */
    class MenuDrawerAdapter : BaseAdapter() {
        val items: Array<String> = arrayOf("Settings", "FileView", "Google Drive", "yande.re", "konachan.com")

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            return TextView(parent?.context).apply {
                textSize = 24F
                gravity = Gravity.CENTER
                padding = 10
                text = items[position]
            }
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

    /**
     * [ListAdapter] for the auto complete search suggestions.
     */
    class TagSearchAutoCompleteAdapter(val recyclerAdapter: ThumbnailAdapter) : BaseAdapter(), Filterable {
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
                    val board = recyclerAdapter.postLoader.board

                    //constraint can be null and FileLoader does not support tag search yet
                    if (!constraint.isNullOrBlank() && recyclerAdapter.postLoader !is FileLoader) {
                        //tags are always lower case
                        val name = constraint.toString().toLowerCase().trim()
                        //request tags
                        val jsonResponse = ApiWrapper.requestTag(board, name)
                        //and parse the result
                        val tags = Gson().fromJson<Array<Tag>>(jsonResponse, Array<Tag>::class.java)

                        //TODO: could be settable
                        val numberOfResults = 10
                        val sortedTagList = mutableListOf<Tag>()

                        //first look if results start with the given search string
                        val primaryMatches = tags.filter { it.name.startsWith(name) }
                        //then look if the second word matches
                        val secondaryMatches = tags.filter {
                            //split into words, drop the first word since it is already covered in primaryMatches
                            val words = it.name.split("_").drop(1)

                            //true if at least one word matches
                            words.filter { it.startsWith(name) }.any()
                        }
                        //then get the list of matches where the search string was only part of a word
                        val restOfResults = tags.subtract(primaryMatches).subtract(secondaryMatches)
                        //add primaryMatches
                        sortedTagList.addAll(primaryMatches.take(numberOfResults))

                        //if not enough results yet, try to fill the rest with secondaryMatches
                        if (sortedTagList.size < numberOfResults)
                            sortedTagList.addAll(secondaryMatches.take(numberOfResults - primaryMatches.size))

                        //if still not enough matches, add the rest
                        if (sortedTagList.size < numberOfResults) {
                            val matchesLeft = numberOfResults - primaryMatches.size - secondaryMatches.size
                            //remove primary and secondary matches to not add them twice
                            sortedTagList.addAll(restOfResults.take(matchesLeft))
                        }

                        results.count = numberOfResults
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
            return TextView(parent.context).apply {
                text = tagList[position].name
                gravity = Gravity.CENTER
                textSize = 20F
                textColor = tagList[position].getTextColor()
                backgroundColor = Color.BLACK
            }
        }
    }
}
