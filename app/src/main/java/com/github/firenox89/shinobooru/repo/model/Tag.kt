package com.github.firenox89.shinobooru.repo.model

import android.graphics.Color
import com.github.firenox89.shinobooru.repo.ApiWrapper
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber
import java.io.Serializable

/**
 * Data class to store the meta information of a tag.
 * Contains some utility functions.
 */
data class Tag(
        val name: String,
        val board: String,
        var id: Long = 0L,
        var count: Int = 0,
        var type: Int = -1,
        var ambiguous: Boolean = false) : Serializable {
    /**
     * Returns a [Color] value as an Int, depending on the tag type.
     *
     * @return an Int value representing the color.
     */
    fun getTextColor(): Int =
            when (type) {
                0 -> Color.parseColor("#EE8887")
                1 -> Color.parseColor("#CCCC00")
                3 -> Color.parseColor("#DD00DD")
                4 -> Color.parseColor("#00AA00")
                5 -> Color.parseColor("#00BBBB")
                6 -> Color.parseColor("#FF2020")
                else -> Color.parseColor("#EE8887")
            }
}