package com.example.imagesqueezer

import android.content.Context
import androidx.preference.PreferenceManager

class PreferenceUtils(private val context: Context) {
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private fun getSavedResolution(): String? {
        return sharedPreferences.getString(context.getString(R.string.preference_resolution_key), "null")
    }

    fun getQualityParameter(): Int {
        return sharedPreferences.getInt(context.getString(R.string.preferences_quality_key), QUALITY_DEFAULT)
    }

    fun getResolutionParameter(): Pair<Float, Float> {
        when (getSavedResolution()) {
            context.getString(R.string.preferences_full_hd_res) -> {
                return Pair(FULL_HD_RESOLUTION_WIDTH, FULL_HD_RESOLUTION_HEIGHT)
            }
            context.getString(R.string.preferences_hd_plus_res) -> {
                return  Pair(HD_PLUS_RESOLUTION_WIDTH, HD_PLUS_RESOLUTION_HEIGHT)
            }
            context.getString(R.string.preferences_hd_res) -> {
                return  Pair(HD_RESOLUTION_WIDTH, HD_RESOLUTION_HEIGHT)
            }
            context.getString(R.string.preferences_sd_res) -> {
                return Pair(SD_RESOLUTION_WIDTH, SD_RESOLUTION_HEIGHT)
            }
            context.getString(R.string.preferences_lowest_res) -> {
                return Pair(LOWEST_RESOLUTION_WIDTH, LOWEST_RESOLUTION_HEIGHT)
            }
        }

        return Pair(LOWEST_RESOLUTION_WIDTH, LOWEST_RESOLUTION_HEIGHT)
    }

    companion object {
        private const val QUALITY_DEFAULT = 75
        private const val LOWEST_RESOLUTION_WIDTH = 800f
        private const val LOWEST_RESOLUTION_HEIGHT = 600f
        private const val SD_RESOLUTION_WIDTH = 720f
        private const val SD_RESOLUTION_HEIGHT = 480f
        private const val HD_RESOLUTION_WIDTH = 1280f
        private const val HD_RESOLUTION_HEIGHT = 720f
        private const val HD_PLUS_RESOLUTION_WIDTH = 1600f
        private const val HD_PLUS_RESOLUTION_HEIGHT = 900f
        private const val FULL_HD_RESOLUTION_WIDTH = 1920f
        private const val FULL_HD_RESOLUTION_HEIGHT = 1080f
    }
}