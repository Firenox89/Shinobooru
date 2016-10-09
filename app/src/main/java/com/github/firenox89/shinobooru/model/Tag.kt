package com.github.firenox89.shinobooru.model

import android.graphics.Color
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable

data class Tag(
        val name: String,
        val board: String,
        var id: Long = 0L,
        var count: Int = 0,
        var type: Int = -1,
        var ambiguous: Boolean = false) : Serializable {

    //TODO: cache on filesystem
    init {
        //load tag info if object was created only by board and name
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
                    val jsonArray = JSONArray(ApiWrapper.requestTag(board, name))
                    var jsonRespone: JSONObject? = null
                    if (jsonArray.length() == 1) {
                        jsonRespone = jsonArray.getJSONObject(0)
                    } else {
                        for (i: Int in 0..jsonArray.length()-1) {
                            jsonRespone = jsonArray.getJSONObject(i)
                            if (jsonRespone.getString("name").equals(name))
                                break
                        }
                    }

                    if (jsonRespone != null) {
                        id = jsonRespone.getLong("id")
                        count = jsonRespone.getInt("count")
                        type = jsonRespone.getInt("type")
                        ambiguous = jsonRespone.getBoolean("ambiguous")
                        tagList.put(name, this)
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun getTextColor(): Int {
        var textColor: Int = Color.parseColor("#EE8887")
        when (type) {
            0 -> textColor = Color.parseColor("#EE8887")
            1 -> textColor = Color.parseColor("#CCCC00")
            3 -> textColor = Color.parseColor("#DD00DD")
            4 -> textColor = Color.parseColor("#00AA00")
            5 -> textColor = Color.parseColor("#00BBBB")
            6 -> textColor = Color.parseColor("#FF2020")
        }
        return textColor
    }

    companion object {
        private val boardTagLists = mutableMapOf<String, MutableMap<String, Tag>>()
    }
}