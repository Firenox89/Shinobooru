package com.github.firenox89.shinobooru.repo.model

import java.util.regex.Pattern

/**
 * Data class to store the meta information of a post.
 * Contains some utility functions.
 */
open class Post(
        var id: Long = 0,
        var tags: String = "",
        var created_at: Int = 0,
        var creator_id: Int = 0,
        var author: String = "",
        var change: Int = 0,
        var source: String = "",
        var score: Int = 0,
        var md5: String = "",
        var file_size: Int = 0,
        var is_shown_in_index: Boolean = false,
        var preview_width: Int = 0,
        var preview_height: Int = 0,
        var actual_preview_width: Int = 0,
        var actual_preview_height: Int = 0,
        var sample_width: Int = 0,
        var sample_height: Int = 0,
        var sample_file_size: Int = 0,
        var jpeg_width: Int = 0,
        var jpeg_height: Int = 0,
        var jpeg_file_size: Int = 0,
        var rating: String = "",
        var has_children: Boolean = false,
        var parent_id: Int = 0,
        var status: String = "",
        var width: Int = 0,
        var height: Int = 0,
        var is_held: Boolean = false,
        var frames_pending_string: String = "",
        //        var frames_pending : Array<String>,
        var frames_string: String = "",
        //        var frames : Array<String>,
        val firstName: String = "",
        val lastName: String = "") {

    /**
     * konachan removed the 'http:' part from their links so we have to add it if it is missing.
     */
    var preview_url: String = ""
        get() = "${if (field.startsWith("//")) "https:" else ""}$field"
    var file_url: String = ""
        get() = "${if (field.startsWith("//")) "https:" else ""}$field"
    var sample_url: String = ""
        get() = "${if (field.startsWith("//")) "https:" else ""}$field"
    var jpeg_url: String = ""
        get() = "${if (field.startsWith("//")) "https:" else ""}$field"

    companion object {
        val boardPattern = Pattern.compile("http[s]?://(?:files\\.)?([a-z.]*)")
    }

    /**
     * Parse and return the board name from the file url.
     *
     * @return the board name this post belongs to.
     */
    open fun getBoard(): String {
        val matcher = boardPattern.matcher(file_url)
        matcher.find()
        return matcher.group(1)
    }

    override fun toString(): String {
        return "Post(id=$id, tags='$tags', created_at=$created_at, creator_id=$creator_id," +
                " author='$author', change=$change, source='$source', score=$score, md5='$md5'," +
                " file_size=$file_size, is_shown_in_index=$is_shown_in_index," +
                " preview_width=$preview_width, preview_height=$preview_height," +
                " actual_preview_width=$actual_preview_width," +
                " actual_preview_height=$actual_preview_height, sample_width=$sample_width," +
                " sample_height=$sample_height, sample_file_size=$sample_file_size," +
                " jpeg_width=$jpeg_width, jpeg_height=$jpeg_height, jpeg_file_size=$jpeg_file_size," +
                " rating='$rating', has_children=$has_children, parent_id=$parent_id," +
                " status='$status', width=$width, height=$height, is_held=$is_held," +
                " frames_pending_string='$frames_pending_string', frames_string='$frames_string'," +
                " firstName='$firstName', lastName='$lastName')"
    }


    fun getTagList(): List<Tag> =
            tags.split(" ").map { Tag(name = it, board = getBoard()) }

    override fun equals(other: Any?): Boolean =
            if (other is Post)
                other.id == id && other.getBoard() == getBoard()
            else
                false
}
