package com.github.firenox89.shinobooru.ui.thumbnail

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.widget.AutoCompleteTextView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope

import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.settings.SettingsActivity
import com.github.firenox89.shinobooru.settings.SettingsManager
import com.github.firenox89.shinobooru.ui.base.BaseActivity
import com.github.firenox89.shinobooru.ui.post.PostPagerActivity
import com.github.firenox89.shinobooru.utility.Constants
import com.github.firenox89.shinobooru.utility.Constants.BOARD_INTENT_KEY
import com.github.firenox89.shinobooru.utility.Constants.POSITION_INTENT_KEY
import com.github.firenox89.shinobooru.utility.Constants.TAGS_INTENT_KEY
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_thumbnail.*
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

class ThumbnailActivity : BaseActivity() {

    private val settingsManager: SettingsManager by inject()
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private var recyclerAdapter: ThumbnailAdapter? = null

    private val recyclerLayout = androidx.recyclerview.widget.StaggeredGridLayoutManager(4, androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL)

    private lateinit var board: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thumbnail)
        setSupportActionBar(findViewById(R.id.my_toolbar))

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)
        }

        //setup the RecyclerView adapter
        val tags = intent.getStringExtra(TAGS_INTENT_KEY) ?: ""
        board = intent.getStringExtra(BOARD_INTENT_KEY) ?: SettingsActivity.currentBoardURL

        setupDrawer(nav_view, drawer_layout, board)

        swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                dataSource.getPostLoader(board, tags).onRefresh()
                swipeRefreshLayout.isRefreshing = false
            }
        }
        title = "${board.replace("https://", "")} $tags"

        Timber.i("board '$board' tags '$tags'")
        lifecycleScope.launch {
            recyclerAdapter = ThumbnailAdapter(lifecycleScope, dataSource.getPostLoader(board, tags)) { clickedPostId ->
                //when an image was clicked start a new PostPagerActivity that starts on Post that was clicked
                Timber.d("Click on post imageID")
                val intent = Intent(this@ThumbnailActivity, PostPagerActivity::class.java)
                intent.putExtra(BOARD_INTENT_KEY, board)
                intent.putExtra(TAGS_INTENT_KEY, tags)
                intent.putExtra(POSITION_INTENT_KEY, clickedPostId)
                startActivityForResult(intent, 1)
            }
            recyclerAdapter?.subscribeLoader()
            recyclerView.layoutManager = recyclerLayout
            Timber.w("Set adapter")
            recyclerView.adapter = recyclerAdapter

            //update the number of posts per row of the recycler layout
            updatePostPerRow()
        }
    }

    override fun onResume() {
        super.onResume()

        //update the number of posts per row of the recycler layout
        updatePostPerRow()
    }

    /**
     * Updates the span count of the layout manager.
     * If the given value is 1 the [RecyclerView] will display sample instead of preview images.
     * @param value that should be used as spanCount
     */
    private fun updatePostPerRow() {
        val postsPerRow = settingsManager.postsPerRow
        recyclerLayout.spanCount = postsPerRow
        recyclerAdapter?.usePreview = postsPerRow != 1
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.thumbnail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                drawer_layout.openDrawer(GravityCompat.START)
                return true
            }
            R.id.search_tags -> {
                return true
            }
        }
        return super.onOptionsItemSelected(item)
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
        super.onActivityResult(requestCode, resultCode, data)
        val post = data?.getIntExtra("position", -1)
        if (post != null && post != -1)
            recyclerView.scrollToPosition(post)
    }



    private fun setupDrawer(navigation: NavigationView, drawer: DrawerLayout, board: String) {
        navigation.inflateHeaderView(R.layout.drawer_header).also { headerView ->
            headerView.findViewById<AutoCompleteTextView>(R.id.tagSearchAutoCompletion).also { autoCompleteTextView ->
                val autoCompleteAdapter = TagSearchAutoCompleteAdapter(board)

                autoCompleteTextView.setAdapter(autoCompleteAdapter)
                autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
                    openBoard(board, autoCompleteAdapter.getItem(position))
                }
            }
        }

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
                    if (board != Constants.FILE_LOADER_NAME) {
                        openBoard(Constants.FILE_LOADER_NAME)
                    }
                }
                else -> {
                    drawer.closeDrawers()
                    if (board != item.title.toString()) {
                        openBoard(item.title.toString())
                    }
                }
            }
            true
        }
    }
}
