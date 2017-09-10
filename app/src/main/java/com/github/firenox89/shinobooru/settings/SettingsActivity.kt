package com.github.firenox89.shinobooru.settings


import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.preference.*
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.app.Shinobooru
import com.github.firenox89.shinobooru.utility.PostLoader
import com.github.salomonbrys.kodein.KodeinInjected
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import rx.subjects.PublishSubject

/**
 * A [PreferenceActivity] that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 *
 *
 * See [
   * Android Design: Settings](http://developer.android.com/design/patterns/settings.html) for design guidelines and the [Settings
   * API Guide](http://developer.android.com/guide/topics/ui/settings.html) for more information on developing a Settings UI.
 */
class SettingsActivity : PreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActionBar()
    }

    /**
     * Set up the [android.app.ActionBar], if the API is available.
     */
    private fun setupActionBar() {
        val actionBar = actionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * {@inheritDoc}
     */
    override fun onIsMultiPane(): Boolean {
        return isXLargeTablet(this)
    }

    /**
     * {@inheritDoc}
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    override fun onBuildHeaders(target: List<PreferenceActivity.Header>) {
        loadHeadersFromResource(R.xml.pref_headers, target)
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    override fun isValidFragment(fragmentName: String): Boolean {
        return PreferenceFragment::class.java.name == fragmentName
                || UIPreferenceFragment::class.java.name == fragmentName
                || RatingPreferenceFragment::class.java.name == fragmentName
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class RatingPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_rating)

            val changeListener = Preference.OnPreferenceChangeListener { preference, any -> PostLoader.ratingChanged(); true }
            findPreference("rating_safe").onPreferenceChangeListener = changeListener
            findPreference("rating_questionable").onPreferenceChangeListener = changeListener
            findPreference("rating_explicit").onPreferenceChangeListener = changeListener
        }
    }

    /**
     * This fragment shows ui preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class UIPreferenceFragment : PreferenceFragment(), KodeinInjected {

        override val injector = KodeinInjector()
        val updateThumbnail : PublishSubject<Int> by instance("thumbnailUpdates")

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            inject(appKodein())
            addPreferencesFromResource(R.xml.pref_ui)
            setHasOptionsMenu(true)

            val changeListener = Preference.OnPreferenceChangeListener { preference, any ->
                updateThumbnail.onNext(any.toString().toInt()); true
            }
            findPreference("post_per_row_list").onPreferenceChangeListener = changeListener
        }
    }

    companion object {
        //boards
        var yandereURL = "https://yande.re"
        var konachanURL = "http://konachan.com"

        var currentBoardURL = yandereURL

        val imageBoards = mutableListOf(yandereURL, konachanURL)

        val pref = PreferenceManager.getDefaultSharedPreferences(Shinobooru.appContext)

        fun filterRating(rating: String): Boolean {
            if (rating.equals("s"))
                return pref.getBoolean("rating_safe", true)
            if (rating.equals("q"))
                return pref.getBoolean("rating_questionable", false)
            if (rating.equals("e"))
                return pref.getBoolean("rating_explicit", false)
            throw IllegalArgumentException("Unknown rating: $rating")
        }

        /**
         * Helper method to determine if the device has an extra-large screen. For
         * example, 10" tablets are extra-large.
         */
        private fun isXLargeTablet(context: Context): Boolean {
            return context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
        }

        //TODO: create Debug Settings
        val  disableCaching: Boolean = false
    }
}
