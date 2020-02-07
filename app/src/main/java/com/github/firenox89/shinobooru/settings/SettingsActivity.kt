package com.github.firenox89.shinobooru.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.firenox89.shinobooru.R
import com.github.firenox89.shinobooru.repo.DataSource
import org.koin.core.KoinComponent
import org.koin.core.inject

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, PreferenceFragment())
                .commit()
    }

    class PreferenceFragment : PreferenceFragmentCompat(), KoinComponent {
        private val dataSource: DataSource by getKoin().inject()

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_general)

            val changeListener = Preference.OnPreferenceChangeListener { _, _ -> dataSource.onRatingChanged(); true }

            findPreference<CheckBoxPreference>("rating_safe")?.onPreferenceChangeListener = changeListener
            findPreference<CheckBoxPreference>("rating_questionable")?.onPreferenceChangeListener = changeListener
            findPreference<CheckBoxPreference>("rating_explicit")?.onPreferenceChangeListener = changeListener
        }
    }

    companion object: KoinComponent {
        //TODO: create Debug Settings
        val  disableCaching: Boolean = false

        //boards
        var yandereURL = "https://yande.re"
        var konachanURL = "http://konachan.com"

        var currentBoardURL = yandereURL

        val imageBoards = mutableListOf(yandereURL, konachanURL)

        private val settingsManager: SettingsManager by inject()

        fun filterRating(rating: String): Boolean {
            if (rating == "s")
                return settingsManager.ratingSafe
            if (rating == "q")
                return settingsManager.ratingQuestionable
            if (rating == "e")
                return settingsManager.ratingExplicit
            throw IllegalArgumentException("Unknown rating: $rating")
        }
    }
}
