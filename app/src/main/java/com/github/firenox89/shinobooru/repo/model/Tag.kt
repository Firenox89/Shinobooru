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
        var ambiguous: Boolean = false) : Serializable, KoinComponent {


    companion object {
        private val boardTagLists = mutableMapOf<String, MutableMap<String, Tag>>()
    }

    val apiWrapper: ApiWrapper by inject()
    /**
     * If object was created only by board and name load tag info.
     * Do nothing otherwise.
     */
    suspend fun loadColor() {
        if (id == 0L) {
            var tagList: MutableMap<String, Tag>
            var tag: Tag
            if (boardTagLists.containsKey(board)) {
                tagList = boardTagLists[board]!!
            } else {
                tagList = mutableMapOf<String, Tag>()
                boardTagLists.put(board, tagList)
            }
            if (tagList.containsKey(name)) {
                tag = tagList[name]!!
                id = tag.id
                count = tag.count
                type = tag.type
                ambiguous = tag.ambiguous
            } else {
                try {
                    val jsonArray = JSONArray(apiWrapper.requestTag(board, name))
                    var jsonRespone: JSONObject? = null

                    //if only one tag was return that has to be the right one
                    if (jsonArray.length() == 1) {
                        jsonRespone = jsonArray.getJSONObject(0)
                    } else {
                        //on multiple matches take the one with the exact name
                        for (i: Int in 0 until jsonArray.length()) {
                            jsonRespone = jsonArray.getJSONObject(i)
                            if (jsonRespone.getString("name") == name)
                                break
                        }
                    }

                    //parse info from json and store in list
                    if (jsonRespone != null) {
                        id = jsonRespone.getLong("id")
                        count = jsonRespone.getInt("count")
                        type = jsonRespone.getInt("type")
                        ambiguous = jsonRespone.getBoolean("ambiguous")
                        tagList.put(name, this)
                    }
                } catch (e: JSONException) {
                    //catch parsing errors just in case...
                    Timber.e(e.message, e)
                }
            }
        }
    }

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