package com.github.firenox89.shinobooru.ui.thumbnail

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.github.firenox89.shinobooru.repo.ApiWrapper
import com.github.firenox89.shinobooru.repo.FileLoader
import com.github.firenox89.shinobooru.repo.model.Tag
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.context.GlobalContext
import org.koin.core.inject

/**
 * [ListAdapter] for the auto complete search suggestions.
 */
class TagSearchAutoCompleteAdapter(val recyclerAdapter: ThumbnailAdapter) : BaseAdapter(), Filterable, KoinComponent {
    val tagList = mutableListOf<Tag>()

    val apiWrapper: ApiWrapper by inject()

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
                GlobalScope.launch {
                    val board = recyclerAdapter.postLoader.board

                    //constraint can be null and FileLoader does not support tag search yet
                    if (!constraint.isNullOrBlank() && recyclerAdapter.postLoader !is FileLoader) {
                        //tags are always lower case
                        val name = constraint.toString().toLowerCase().trim()
                        //request tags
                        val jsonResponse = apiWrapper.requestTag(board, name)
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
//                textColor = tagList[position].getTextColor()
//                backgroundColor = Color.BLACK
        }
    }
}