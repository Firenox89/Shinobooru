package com.github.firenox89.shinobooru.ui.thumbnail

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBar
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem

import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.settings.SettingsActivity
import com.github.firenox89.shinobooru.ui.GoogleSignInActivity
import com.github.firenox89.shinobooru.ui.base.RxActivity
import com.github.firenox89.shinobooru.ui.post.PostPagerActivity
import com.github.firenox89.shinobooru.utility.Constants.BOARD_INTENT_KEY
import com.github.firenox89.shinobooru.utility.Constants.FILE_LOADER_NAME
import com.github.firenox89.shinobooru.utility.Constants.POSITION_INTENT_KEY
import com.github.firenox89.shinobooru.utility.Constants.TAGS_INTENT_KEY
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_thumbnail.*
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import timber.log.Timber

class ThumbnailActivity : RxActivity() {
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private lateinit var recyclerAdapter: ThumbnailAdapter

    private val sharedPrefs: SharedPreferences by inject()
    private val updateThumbnail: PublishSubject<Int> by inject("thumbnailUpdates")

    private val recyclerLayout = StaggeredGridLayoutManager(4, StaggeredGridLayoutManager.VERTICAL)

    val boards = listOf("yande.re", "konachan.com", "moe.booru.org", "danbooru.donmai.us", "gelbooru.com")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thumbnail)
        setSupportActionBar(findViewById(R.id.my_toolbar))
        val actionbar: ActionBar? = supportActionBar
        actionbar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)
        }

        setupDrawer()

        //setup the RecyclerView adapter
        val tags = intent.getStringExtra(TAGS_INTENT_KEY) ?: ""
        val board = intent.getStringExtra(BOARD_INTENT_KEY) ?: SettingsActivity.currentBoardURL

        title = "${board.replace("https://", "")} $tags"

        Timber.i("start board '$board' tags '$tags'")
        recyclerAdapter = ThumbnailAdapter(get(), board, tags)

        subscribe(recyclerAdapter.subscribeLoader())
        //when an image was clicked start a new PostPagerActivity that starts on Post that was clicked
        subscribe(recyclerAdapter.onImageClickStream.subscribe {
            val intent = Intent(this, PostPagerActivity::class.java)
            intent.putExtra(BOARD_INTENT_KEY, board)
            intent.putExtra(TAGS_INTENT_KEY, tags)
            intent.putExtra(POSITION_INTENT_KEY, it)
            startActivityForResult(intent, 1)
        })

        recyclerView.layoutManager = recyclerLayout
        recyclerView.adapter = recyclerAdapter

        //update the number of posts per row of the recycler layout
        updatePostPerRow(sharedPrefs.getString("post_per_row_list", "3").toInt())
        subscribe(updateThumbnail.subscribe { updatePostPerRow(it) })
    }

    fun setupDrawer() {
        boards.forEach {
            nav_view.menu.add(it)
        }
        nav_view.setNavigationItemSelectedListener { item ->
            when (item.title) {
                "Settings" -> {
                    drawer_layout.closeDrawers()
                    openSettings()
                }
                "Sync" -> {
                    drawer_layout.closeDrawers()
                    openGoogleDriveView()
                }
                "Filemanager" -> {
                    drawer_layout.closeDrawers()
                    openBoard(FILE_LOADER_NAME)
                }
                else -> {
                    drawer_layout.closeDrawers()
                    openBoard(item.title.toString())
                }
            }
            true
        }
    }

    /**
     * Starts the [SyncActivity].
     */
    private fun openGoogleDriveView() {
        val intent = Intent(this, GoogleSignInActivity::class.java)
        startActivity(intent)
    }

    /**
     * Starts the [SyncActivity].
     */
    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun openBoard(board: String) {
        val intent = Intent(this, ThumbnailActivity::class.java)
        intent.putExtra(BOARD_INTENT_KEY, board)
        startActivity(intent)
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.thumbnail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            android.R.id.home -> {
                drawer_layout.openDrawer(GravityCompat.START)
                return true
            }
            R.id.settings -> {
                return true
            }
            R.id.search_tags -> {
                finish()
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
        val post = data?.getIntExtra("position", -1)
        if (post != null && post != -1)
            recyclerView.scrollToPosition(post)
    }
}
