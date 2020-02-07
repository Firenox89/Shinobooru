package com.github.firenox89.shinobooru.settings

import android.content.SharedPreferences
import android.net.Uri
import androidx.core.net.toUri

class SettingsManager(private val sharedPreferences: SharedPreferences) {
    val postsPerRow
        get() = sharedPreferences.getString("post_per_row_list", "3")?.toInt() ?: 3
    val ratingSafe
        get() = sharedPreferences.getBoolean("rating_safe", true)
    val ratingQuestionable
        get() = sharedPreferences.getBoolean("rating_questionable", false)
    val ratingExplicit
        get() = sharedPreferences.getBoolean("rating_explicit", false)
    val nextcloudBaseUri: Uri?
        get() = sharedPreferences.getString("nextcloud_uri", null)?.toUri()
    val nextcloudUser: String?
        get() = sharedPreferences.getString("nextcloud_user", null)
    val nextcloudPassword: String?
        get() = sharedPreferences.getString("nextcloud_password", null)
}