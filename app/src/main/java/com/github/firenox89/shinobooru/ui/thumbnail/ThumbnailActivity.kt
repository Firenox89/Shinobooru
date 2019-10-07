package com.github.firenox89.shinobooru.ui.thumbnail

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBar
import androidx.recyclerview.widget.RecyclerView
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem

import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.settings.SettingsActivity
import com.github.firenox89.shinobooru.ui.base.BaseActivity
import com.github.firenox89.shinobooru.ui.post.PostPagerActivity
import com.github.firenox89.shinobooru.utility.Constants.BOARD_INTENT_KEY
import com.github.firenox89.shinobooru.utility.Constants.POSITION_INTENT_KEY
import com.github.firenox89.shinobooru.utility.Constants.TAGS_INTENT_KEY
import kotlinx.android.synthetic.main.activity_thumbnail.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

class ThumbnailActivity : BaseActivity() {
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private lateinit var recyclerAdapter: ThumbnailAdapter

    private val sharedPrefs: SharedPreferences by inject()

    private val recyclerLayout = androidx.recyclerview.widget.StaggeredGridLayoutManager(4, androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thumbnail)
        setSupportActionBar(findViewById(R.id.my_toolbar))
        val actionbar: ActionBar? = supportActionBar
        actionbar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)
        }

        setupDrawer(nav_view, drawer_layout)

        //setup the RecyclerView adapter
        val tags = intent.getStringExtra(TAGS_INTENT_KEY) ?: ""
        val board = intent.getStringExtra(BOARD_INTENT_KEY) ?: SettingsActivity.currentBoardURL

        title = "${board.replace("https://", "")} $tags"

        Timber.i("start board '$board' tags '$tags'")
        recyclerAdapter = ThumbnailAdapter(dataSource, board, tags)

        GlobalScope.launch {
            recyclerAdapter.subscribeLoader()
            //when an image was clicked start a new PostPagerActivity that starts on Post that was clicked
            for (imageID in recyclerAdapter.onImageClickStream) {
                val intent = Intent(this@ThumbnailActivity, PostPagerActivity::class.java)
                intent.putExtra(BOARD_INTENT_KEY, board)
                intent.putExtra(TAGS_INTENT_KEY, tags)
                intent.putExtra(POSITION_INTENT_KEY, imageID)
                startActivityForResult(intent, 1)
            }
        }

        recyclerView.layoutManager = recyclerLayout
        recyclerView.adapter = recyclerAdapter

        //update the number of posts per row of the recycler layout
        updatePostPerRow(sharedPrefs.getString("post_per_row_list", "3").toInt())
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
}
