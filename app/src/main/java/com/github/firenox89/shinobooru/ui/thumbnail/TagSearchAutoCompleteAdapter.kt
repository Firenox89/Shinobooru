package com.github.firenox89.shinobooru.ui.thumbnail

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.github.firenox89.shinobooru.repo.DataSource
import com.github.firenox89.shinobooru.repo.model.Tag
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber
import java.util.*

class TagSearchAutoCompleteAdapter(val board: String) : BaseAdapter(), Filterable, KoinComponent {
    val tagList = mutableListOf<Tag>()

    val dataSource: DataSource by inject()

    override fun getItem(position: Int): String {
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
                return runBlocking {
                    val results = FilterResults()
                    //constraint can be null and FileLoader does not support tag search yet
                    if (!constraint.isNullOrBlank()) {
                        //tags are always lower case
                        val name = constraint.toString().toLowerCase().trim()
                        //request tags
                        val tags = dataSource.tagSearch(board, name)

                        Timber.d("Found tags $tags")
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
                    results
                }
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
            this.setTextColor(tagList[position].getTextColor())
//                backgroundColor = Color.BLACK
        }
    }
}