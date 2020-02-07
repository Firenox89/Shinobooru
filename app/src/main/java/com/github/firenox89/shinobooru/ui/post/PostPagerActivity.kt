package com.github.firenox89.shinobooru.ui.post

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.repo.PostLoader
import com.github.firenox89.shinobooru.repo.StoragePostLoader
import com.github.firenox89.shinobooru.repo.model.DownloadedPost
import com.github.firenox89.shinobooru.repo.model.Post
import com.github.firenox89.shinobooru.ui.base.BaseActivity
import com.github.firenox89.shinobooru.ui.showConfirmationDialog
import com.github.firenox89.shinobooru.ui.showToast
import com.github.firenox89.shinobooru.utility.Constants.BOARD_INTENT_KEY
import com.github.firenox89.shinobooru.utility.Constants.POSITION_INTENT_KEY
import com.github.firenox89.shinobooru.utility.Constants.TAGS_INTENT_KEY
import com.github.kittinunf.result.map
import kotlinx.android.synthetic.main.activity_post_pager.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Creates a [VerticalViewPager] that starts on a given [Post] and load new posts from the given [PostLoader]
 */
class PostPagerActivity : BaseActivity() {

    private lateinit var postLoader: PostLoader
    lateinit var board: String
    lateinit var tags: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_pager)

        //better fail here then later
        board = intent.getStringExtra(BOARD_INTENT_KEY)
                ?: throw IllegalArgumentException("Postloader can't be null")
        //TODO: check if there are post available for the tags, switching to an empty Activity is kinda stupid...
        tags = intent.getStringExtra(TAGS_INTENT_KEY) ?: ""
        title = "${board.replace("https://", "")} $tags"

        lifecycleScope.launch {
            postLoader = dataSource.getPostLoader(board, tags)

            val position = intent.getIntExtra(POSITION_INTENT_KEY, -1)
            require(position != -1) { "Position must not be null" }

            val postAdapter = PostPagerAdapter(supportFragmentManager, lifecycleScope, postLoader) { newPosition ->
                //set activity result to current post for scrolling in thumbnail view
                //nothing to fail here so result is always ok
                setResult(Activity.RESULT_OK, Intent().apply {
                    putExtra("position", newPosition)
                })
            }
            postAdapter.subscribeLoader()

            Timber.i("Start post page on page $position")
            postviewpager.adapter = postAdapter
            postviewpager.currentItem = position
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (postLoader is StoragePostLoader) {
            menuInflater.inflate(R.menu.delete, menu)
        } else {
            menuInflater.inflate(R.menu.download, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_download -> {
                Toast.makeText(this, "Downloading...", Toast.LENGTH_LONG).show()
                GlobalScope.launch {
                    val post = postLoader.getPostAt(postviewpager.currentItem)
                    dataSource.downloadPost(post).fold({
                        showToast(this@PostPagerActivity, "Download successful")
                    }, {
                        showToast(this@PostPagerActivity, "Download failed, ${it.message}")
                        Timber.e(it, "Download failed $post")
                    })
                }
                return true
            }
            R.id.action_delete -> {
                GlobalScope.launch {
                    showConfirmationDialog(this@PostPagerActivity, R.string.confirm, R.string.really_delete).map {
                        val post = postLoader.getPostAt(postviewpager.currentItem)
                        dataSource.deletePost(post as DownloadedPost).fold({
                            showToast(this@PostPagerActivity, "Deleted")
                            //we close the activity since the displayed post was deleted
                            withContext(Dispatchers.Main) {
                                this@PostPagerActivity.finish()
                            }
                        }, {
                            showToast(this@PostPagerActivity, "Delete failed ${it.message}")
                            Timber.e(it, "Delete failed $post")
                        })
                    }
                }
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
