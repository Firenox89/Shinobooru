package com.github.firenox89.shinobooru.ui.post

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.github.firenox89.shinobooru.repo.PostLoader
import kotlinx.coroutines.*
import timber.log.Timber

class PostPagerAdapter(
        fm: FragmentManager,
        private val lifecycleScoop: CoroutineScope,
        private val postLoader: PostLoader,
        private val currentPostUpdater: (Int) -> Unit)
    : androidx.fragment.app.FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private var size = postLoader.getCount()

    suspend fun subscribeLoader() {
        lifecycleScoop.launch {
            for (change in postLoader.getRangeChangeEventStream()) {
                Timber.w("Update $change")
                withContext(Dispatchers.Main) {
                    size = change.first + change.second
                    notifyDataSetChanged()
                }
            }
        }
    }

    override fun getItem(position: Int): Fragment {
        lifecycleScoop.launch {
            //request new posts when less then 5 posts are left to load
            if (postLoader.getCount() - position < 5) postLoader.requestNextPosts()
        }

        currentPostUpdater.invoke(position)

        Timber.w("Build post frag $position")
        return PostFragment().apply {
            arguments = Bundle().apply {
                putString("board", postLoader.board)
                putString("tags", postLoader.tags)
                putInt("posi", position)
            }
        }
    }

    override fun getCount(): Int {
        return size
    }
}