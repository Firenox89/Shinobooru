package com.github.firenox89.shinobooru.ui.post

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.repo.PostLoader
import com.github.firenox89.shinobooru.repo.model.Post
import com.github.firenox89.shinobooru.ui.base.BaseActivity
import com.github.firenox89.shinobooru.utility.Constants.BOARD_INTENT_KEY
import com.github.firenox89.shinobooru.utility.Constants.POSITION_INTENT_KEY
import com.github.firenox89.shinobooru.utility.Constants.TAGS_INTENT_KEY
import kotlinx.android.synthetic.main.activity_post_pager.*
import timber.log.Timber


/**
 * Creates a [VerticalViewPager] that starts on a given [Post] and load new posts from the given [PostLoader]
 */
class PostPagerActivity : BaseActivity() {

    private lateinit var postLoader: PostLoader
    lateinit var board: String
    lateinit var tags: String
    /**
     * Creates a [VerticalViewPager] that starts on a given [Post] and load new posts from the given [PostLoader]
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_pager)

        //better fail here then later
        board = intent.getStringExtra(BOARD_INTENT_KEY) ?: throw IllegalArgumentException("Postloader can't be null")
        //TODO: check if there are post available for the tags, switching to an empty Activity is kinda stupid...
        tags = intent.getStringExtra(TAGS_INTENT_KEY) ?: ""
        title = "${board.replace("https://", "")} $tags"

        postLoader = dataSource.getPostLoader(board, tags)

        val position = intent.getIntExtra(POSITION_INTENT_KEY, -1)
        require(position != -1) { "Position must not be null" }

        Timber.i("Start post page on page $position")
        with(postviewpager) {
            adapter = PostPagerAdapter(supportFragmentManager, postLoader, this@PostPagerActivity)
            currentItem = position
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.download, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_download -> {
                postLoader.downloadPost(postviewpager.currentItem)
                Toast.makeText(this, "Downloading...", Toast.LENGTH_LONG).show()
                return true
            }
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
